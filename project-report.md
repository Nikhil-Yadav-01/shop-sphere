# ShopSphere Grocery Backend -- Production Readiness Audit Report

**Date:** 2026-05-03
**Auditor:** Principal Distributed Systems Architect
**Scope:** Full codebase audit of 22 microservices, infrastructure, gateway, and cross-cutting concerns
**Target Scale:** Millions of users, Big-Billion-Day-level traffic spikes

---

## Executive Summary

ShopSphere Grocery is a 22-service Spring Boot 3.2.0 microservices architecture with Spring Cloud Gateway, Netflix Eureka, PostgreSQL, MongoDB, Redis, and Apache Kafka. On paper, the architecture is well-structured with clear service boundaries and per-service databases. In practice, **the system would catastrophically fail under production load at scale**.

The project contains serious architectural anti-patterns, missing critical infrastructure, zero test coverage, no security enforcement, no resilience mechanisms, and numerous data race conditions. Many services contain stub code, dead dependencies, and incomplete features.

**Production Readiness Score: 2/10**

Justification: The project demonstrates good foundational architectural decisions (microservice separation, per-service databases, API Gateway, service discovery, Kafka), but lacks nearly every mechanism required to survive production traffic: no tests, no circuit breakers, no rate limiting, no distributed tracing, no centralized logging, no authentication between services, no idempotency, no caching layer for read-heavy services, and critical concurrency bugs in inventory, cart, checkout, and coupon services. The payment service has no real gateway integration. Several services are essentially stubs.

---

## Top 10 Critical Fixes

| # | Issue | Impact | Fix |
|---|---|---|---|
| 1 | **Zero test coverage across all 22 services** | No safety net for any change; production regressions guaranteed | Add unit, integration, and contract tests with 80%+ coverage minimum |
| 2 | **No rate limiting on API Gateway or auth endpoints** | Brute-force attacks, credential stuffing, API abuse, cascading failures during spikes | Implement Redis-backed RequestRateLimiter in Spring Cloud Gateway + bucket4j on auth-service |
| 3 | **No inter-service authentication** | Any compromised service can access/modify any other service's data; lateral movement trivial | Implement mTLS between services or JWT-based service-to-service auth with SPIFFE/SPIRE |
| 4 | **Inventory race conditions** (no concurrency protection on REST endpoints) | Overselling during flash sales; stock goes negative | Add optimistic locking (`@Version`) + distributed Redis locks on all mutation endpoints, not just Kafka listeners |
| 5 | **Cart race condition + hardcoded prices** | Concurrent cart writes lose data; product prices are `BigDecimal("99.99")` | Fix check-then-act race with Redis SETNX/SET NX; integrate with catalog/pricing service for real prices |
| 6 | **Payment: no idempotency, no real gateway, `create-drop` DDL** | Duplicate charges, data loss on restart, 100% stub processing | Add idempotency key validation, integrate real payment gateway (Stripe/Razorpay), set `ddl-auto: validate`, enable Flyway |
| 7 | **Checkout: no Kafka events, no inventory reservation, hardcoded user ID** | Orders placed never notify downstream services; overselling guaranteed; all orders attributed to same user | Add Kafka producer for checkout events, integrate inventory reservation via Feign/gRPC, implement real JWT extraction |
| 8 | **Notification service: no Kafka consumers, no actual email/SMS sending** | Users never receive order confirmations, password resets, or shipping notifications | Add Kafka consumers for order/payment events; integrate with SES/SendGrid/Twilio |
| 9 | **Coupon race condition on `usedCount`** | Usage limits can be exceeded under concurrent access, causing unlimited discount abuse | Add `@Version` optimistic locking + Redis distributed lock or atomic DB increment with `UPDATE coupon SET used_count = used_count + 1 WHERE id = ? AND used_count < usage_limit` |
| 10 | **No CI/CD pipeline** | Manual builds, no automated testing, no blue-green/canary deployments, no rollback capability | Implement GitHub Actions or GitLab CI with build/test/scan/deploy stages; container registry; Helm charts for Kubernetes |

---

## 1. Microservices Architecture

### 1.1 Service Boundaries Analysis

| Service | Bounded Context | Status | Assessment |
|---|---|---|---|
| `auth-service` | Authentication & Identity | Exists, incomplete | JWT dual implementation bug, filter not wired |
| `user-service` | User Profile Management | Exists, incomplete | No Kafka integration per spec |
| `catalog-service` | Product Catalog | Exists, incomplete | Price field lost in mapping, variants unused |
| `inventory-service` | Stock Management | Exists, critical bugs | Race conditions on REST endpoints |
| `cart-service` | Shopping Cart | Exists, critical bugs | Hardcoded prices, race conditions |
| `checkout-service` | Checkout Orchestration | Exists, severely incomplete | No Kafka, no inventory check, stub auth |
| `order-service` | Order Management | Exists, incomplete | No saga pattern, fire-and-forget events |
| `payment-service` | Payment Processing | Exists, stub | `Math.random()` for payment processing |
| `pricing-service` | Price Calculation | Exists, incomplete | Kafka consumer is no-op, no price cache |
| `coupon-service` | Discount Management | Exists, critical bugs | Race condition on usage count |
| `notification-service` | Multi-channel Notifications | Exists, non-functional | No Kafka consumers, no email/SMS integration |
| `search-service` | Product Search | Exists, incomplete | No full-text scoring, reindex is empty |
| `review-service` | Product Reviews | Exists, incomplete | No Kafka events, no duplicate prevention |
| `recommendation-service` | Product Recommendations | Exists, non-functional | No algorithm, no Kafka consumers |
| `returns-service` | Return/Refund Processing | Exists, incomplete | No payment integration, no order verification |
| `fraud-service` | Fraud Detection | Exists, incomplete | Hardcoded rules, `create-drop` DDL |
| `analytics-service` | Event Analytics | Exists, incomplete | No pagination, event loss on crash |
| `media-service` | File/Image Management | Exists, incomplete | Local disk storage, no S3 integration |
| `admin-service` | Admin Dashboard | Exists, severely incomplete | Dashboard hardcoded zeros, audit logs unprotected |
| `batch-service` | Scheduled Jobs | Exists, stub | All jobs are `Thread.sleep()` stubs |
| `api-gateway` | API Gateway | Exists, incomplete | No rate limiting, no CORS, no role checking |
| `discovery-service` | Service Registry | Exists, functional | Single point of failure |

### 1.2 Missing Services per Spec

| Service (per dependency.txt) | Status | Impact |
|---|---|---|
| Config-Server | **NOT IMPLEMENTED** | No centralized configuration management; every service configured individually |
| Shipping/Delivery Service | **NOT IMPLEMENTED** | No logistics/delivery tracking functionality |
| WebSocket-Chat Service | **NOT IMPLEMENTED** | No real-time customer support |
| Monitoring Stack (Prometheus/Grafana) | **NOT IMPLEMENTED** | No production observability |

### 1.3 Inter-Service Communication

**Current State:**

| Pattern | Usage | Assessment |
|---|---|---|
| REST (synchronous) | API Gateway -> Services | Present but without circuit breakers, timeouts, or retries |
| Kafka (async) | Event publishing | Partially implemented; many producers never called, consumers missing |
| gRPC | Cart service | **NOT IMPLEMENTED** -- proto file exists, no gRPC service implementation |

**Issues:**

- **No messaging contract/schema registry.** Events use raw `Map<String, Object>` or colon-delimited strings instead of Avro/Protobuf.
- **Fire-and-forget Kafka everywhere.** No `acks=all`, no retries, no dead-letter topics.
- **No request-response pattern** for critical paths (inventory check during checkout).
- **Missing service-to-service clients.** Checkout service has no Feign/gRPC client to call inventory, payment, or cart services.
- **No event versioning.** No schema version in event payloads, making backward-compatible evolution impossible.

### 1.4 API Gateway Issues

| Issue | Severity | Detail |
|---|---|---|
| No rate limiting | CRITICAL | No `RequestRateLimiter` filter; gateway offers zero abuse protection |
| No CORS configuration | HIGH | Browser frontends cannot reach the gateway from different origins |
| No role-based filtering | HIGH | `AuthenticationFilter` only validates token existence, no role/permission checks |
| Synchronous token validation on every request | MEDIUM | Every protected request calls auth-service via HTTP; no JWT self-validation or caching |
| Inconsistent service ID casing | LOW | Mix of `lb://auth-service` and `lb://CATALOG-SERVICE` |
| Auth service URL bypasses service discovery | MEDIUM | Token validation uses direct `http://localhost:8081` not `lb://auth-service` |
| Empty 401 response bodies | LOW | Clients cannot distinguish "no token" vs "invalid token" vs "auth service down" |

### 1.5 Service Discovery

- **Netflix Eureka** is the single registry. Every service depends on it being healthy.
- **No fallback mechanism.** If Eureka goes down, no new service registrations and stale route cache.
- **Eureka is a single point of failure** -- only one instance is configured.
- **No alternative** (e.g., Consul, Kubernetes native service discovery) is planned.

**Fix:** Deploy Eureka as a 3-node cluster with peer awareness, or migrate to Kubernetes-native service discovery with CoreDNS.

---

## 2. Scalability & Performance

### 2.1 Horizontal Scaling Strategy

**Current State: NOT READY**

| Concern | Status | Detail |
|---|---|---|
| Stateless services | Partial | Cart service stores state in Redis (good). Order, Payment, User services store in DB (good). But checkout-service hardcodes user ID, inventory-service holds state in local Redis locks. |
| Autoscaling | NOT IMPLEMENTED | No HPA/VPA configs, no Kubernetes manifests, no resource limits in docker-compose |
| Load balancing | Partial | Spring Cloud LoadBalancer via `lb://`, but no circuit breakers, no retry policies |
| Connection pooling | Default only | HikariCP defaults used everywhere; no tuning for high-throughput scenarios |
| JVM memory limits | Missing | Most Dockerfiles lack `-XX:MaxRAMPercentage`; risk of OOM kills |

### 2.2 Caching

| Service | Expected Cache | Current Status | Gap |
|---|---|---|---|
| Catalog (read-heavy) | Redis/CDN for product listings | **NO CACHING** | Every read hits MongoDB directly |
| Pricing | Redis for price calculations | **NO CACHING** | 3+ DB queries per price calculation |
| User profile | Redis for session/profile | **NO CACHING** | Every profile lookup hits PostgreSQL |
| Coupon | Redis for validation | **NO CACHING** | Every validation hits PostgreSQL |
| Search | MongoDB indexes only | Partial | No result caching |

**Impact:** During flash sales, catalog and pricing services will become database bottlenecks. A catalog with 100K products viewed by 1M concurrent users = 1M MongoDB queries per page load.

### 2.3 Database Scaling

| Concern | Status | Assessment |
|---|---|---|
| Read replicas | NOT CONFIGURED | All services use single PostgreSQL/MongoDB instances |
| Sharding | NOT PLANNED | No sharding strategy for orders, events, or analytics at scale |
| CQRS | NOT IMPLEMENTED | Order service uses same model for reads and writes |
| Pagination | Missing on critical endpoints | `GET /order` returns ALL orders; `GET /payment` returns ALL payments; `getAllCategories` unbounded |
| Connection pool tuning | DEFAULT | HikariCP default pool size (10) is insufficient for high-throughput services |

### 2.4 Async Processing

| Concern | Status | Assessment |
|---|---|---|
| Kafka topic configuration | 1 partition, auto-create | Single partition per topic = no parallelism for consumers |
| Producer acknowledgments | Not configured | Default `acks=1`; should be `acks=all` for critical events |
| Producer retries | Not configured | Transient broker failures cause event loss |
| Dead-letter topics | NOT IMPLEMENTED | Failed messages are silently dropped everywhere |
| Consumer auto-commit | Enabled in analytics | Can lose events on crash |
| Consumer groups | Some services have them | No partition-aware consumer group strategy |

### 2.5 Latency Bottlenecks

| Bottleneck | Service | Detail |
|---|---|---|
| Synchronous token validation per request | API Gateway | Every protected request adds 50-200ms for auth-service HTTP call |
| Long-running DB transaction during payment gateway call | Payment Service | `@Transactional` holds DB lock during simulated gateway call |
| Sequential rule evaluation for pricing | Pricing Service | No caching, 3+ DB queries per price calculation |
| Unpaginated queries returning all records | Order, Payment, Analytics, Notification | N+1 queries and OOM risk |
| No CDN for media/images | Media Service | Files served from local disk; every image request hits the service |

---

## 3. Data & Consistency

### 3.1 Database per Service

**Good:** Each service has its own database (PostgreSQL or MongoDB). No shared databases detected.

**Concerns:**

| Issue | Detail |
|---|---|
| 13 separate PostgreSQL instances | Operationally expensive; at scale, should consolidate to a managed PostgreSQL cluster with schema-level isolation |
| No read replicas | Every service reads from its primary |
| No database backup strategy | docker-compose volumes only; no automated backup/restore |
| Flyway disabled in several services | Payment (`flyway.enabled: false`), Notification (`flyway.enabled: false`), Fraud (`flyway.enabled: false`), User (no migration files exist) |

### 3.2 Transaction Management (SAGA / Eventual Consistency)

**CRITICAL GAP: No SAGA pattern is implemented.**

Order creation flow should be:
1. Create order (PENDING)
2. Reserve inventory
3. Process payment
4. Confirm order

**Current implementation:** Order service creates the order and publishes a fire-and-forget Kafka event. Nothing coordinates inventory reservation or payment. The checkout service does not call inventory or payment. The inventory service consumes `order.placed` but has no idempotency or compensation on partial failure.

**Scenario that WILL happen at scale:**
1. Order is created in DB
2. `order-events` Kafka message published
3. Inventory service receives `order.placed`, reserves item #1 successfully
4. Item #2 fails (out of stock) -- `RuntimeException` thrown
5. Kafka consumer retries -- item #1 already reserved, now double-reserved
6. Order is in DB as PENDING, inventory is inconsistent, customer is charged nothing

### 3.3 Idempotency

| Service | Idempotency Status | Risk |
|---|---|---|
| Order creation | NO IDEMPOTENCY | Duplicate orders on network retry |
| Payment processing | NO IDEMPOTENCY | Duplicate charges on network retry |
| Inventory reservation (Kafka) | NO IDEMPOTENCY | Double-reservation on consumer retry |
| User registration | Partial (email uniqueness check) | Concurrent registrations with same email handled |
| Cart operations | NO IDEMPOTENCY | Race condition in getOrCreateCart |
| Coupon application | NO IDEMPOTENCY | Race condition on usedCount |

### 3.4 Schema Migrations

| Service | Flyway Status | Issue |
|---|---|---|
| auth-service | Enabled (3 migrations) | OK |
| admin-service | Enabled (1 migration) | OK |
| inventory-service | Enabled (1 migration) | OK |
| order-service | Enabled (2 migrations) | OK |
| payment-service | **DISABLED** | Migration exists but flyway.enabled=false |
| user-service | **NO MIGRATIONS** | No files in db/migration/ |
| notification-service | **DISABLED** | Migration exists but flyway.enabled=false |
| fraud-service | **DISABLED** | Migration exists but flyway.enabled=false |
| media-service | Enabled (1 migration) | OK |
| coupon-service | Enabled (1 migration) | OK |
| pricing-service | Enabled (1 migration) | OK |
| catalog-service | MongoDB (no Flyway) | N/A but no index management |
| review-service | MongoDB (no Flyway) | N/A but no index management |
| returns-service | MongoDB (no Flyway) | N/A |
| analytics-service | MongoDB (no Flyway) | N/A but no TTL indexes |
| recommendation-service | MongoDB (no Flyway) | N/A |
| search-service | MongoDB (no Flyway) | N/A |

---

## 4. Resilience & Fault Tolerance

### 4.1 Circuit Breakers

**NOT IMPLEMENTED.** No Resilience4j dependency in any service.

| Dependency | Circuit Breaker Needed | Current State |
|---|---|---|
| Gateway -> Auth (token validation) | YES | No protection; if auth-service is down, all protected routes fail |
| Checkout -> Cart | YES | No protection |
| Checkout -> Payment | YES | No protection |
| Checkout -> Inventory | YES | No inventory call exists (missing entirely) |
| Order -> Kafka | YES | Fire-and-forget with swallowed exceptions |
| Inventory -> Kafka | YES | Fire-and-forget with swallowed exceptions |
| Any service -> Kafka | YES | All services have this gap |

### 4.2 Retry Policies

**NOT CONFIGURED.**

| Concern | Status |
|---|---|
| Kafka producer retries | Not configured; transient broker failures cause event loss |
| HTTP client retries | No RestTemplate/WebClient retry configuration in any service |
| Database connection retries | HikariCP defaults only |
| Idempotent retries | No idempotency keys exist, so retries are unsafe |

### 4.3 Timeouts

| Service | Timeout Configuration | Status |
|---|---|---|
| Gateway -> Auth | None | No timeout on token validation call; could hang indefinitely |
| Kafka producer | Default (60s block) | Too long for production |
| HTTP clients | Feign defaults in cart-service (unused) | No actual client configured |
| Database | HikariCP default (30s) | Acceptable but should be tuned per service |

### 4.4 Bulkheads

**NOT IMPLEMENTED.** No thread pool isolation between different downstream dependencies.

### 4.5 Graceful Degradation

| Scenario | Expected Behavior | Current Behavior |
|---|---|---|
| Auth service down | Serve cached/valid tokens; allow read-only operations | All protected routes return 401 |
| Kafka down | Queue events locally; retry later | Events silently lost |
| Inventory service down | Show "checking availability"; prevent checkout | Checkout service has no inventory call |
| Redis down | Fail open for cart; fall back to DB | Cart service has no fallback |
| Catalog service down | Serve cached product data | No caching; returns 5xx |

---

## 5. Security

### 5.1 Authentication Architecture

| Component | Status | Issues |
|---|---|---|
| JWT implementation | **BROKEN** | Two incompatible JWT implementations in auth-service: `JwtTokenProvider` (shared) generates `{sub, roles}`, but `AuthServiceImpl` generates `{sub, email, role}`. The `JwtAuthenticationFilter` expects the shared format, so it will reject tokens created by the auth service itself. |
| JWT filter registration | **NOT REGISTERED** | `JwtAuthenticationFilter` exists as `@Component` but is NEVER added to `SecurityFilterChain`. Protected endpoints accept requests without any token validation. |
| JWT secret | **DANGEROUS DEFAULT** | `${jwt.secret:defaultSecretKeyThatShouldBeChangedInProduction123456}` -- visible in source code |
| BCrypt strength | Default (10 rounds) | Should be 12+ for production |
| Refresh tokens | Rotating (good) | Stored in PostgreSQL; no Redis cache for fast lookup |
| Key rotation | Interface exists, NO implementation | Cannot rotate JWT secrets without downtime |

### 5.2 Authorization (RBAC)

**NOT ENFORCED.**

| Component | Status |
|---|---|
| API Gateway role checking | No role extraction or verification; `AuthenticationFilter` only validates token presence |
| Service-level auth | Most services permit all requests (`anyRequest().permitAll()`) |
| Admin endpoints | No ADMIN role verification; anyone can access admin-service |
| CRUD operations on catalog | POST/PUT/DELETE on products and categories are public |
| Review creation | Unauthenticated users can create reviews |
| Payment processing | No auth on payment endpoints |

### 5.3 Service-to-Service Communication

| Concern | Status |
|---|---|
| mTLS | NOT IMPLEMENTED -- all communication is plaintext HTTP |
| Internal auth tokens | NOT IMPLEMENTED -- services trust any request from the internal network |
| Kafka security | PLAINTEXT -- no SASL, no SSL |
| Database connections | No SSL/TLS; passwords in environment variables |

### 5.4 Secrets Management

| Concern | Status |
|---|---|
| Database passwords | Hardcoded default: `password` in application.yml and docker-compose.yml |
| JWT secret | Hardcoded in docker-compose.yml |
| Payment API keys | Hardcoded: `mock-api-key` |
| SMTP credentials | Hardcoded in application-local.yml |
| Docker secrets | NOT USED |
| HashiCorp Vault / AWS Secrets Manager | NOT INTEGRATED |

### 5.5 OWASP Vulnerabilities

| Vulnerability | Status |
|---|---|
| SQL Injection | Low risk (JPA parameterized queries) but raw queries in some migration scripts |
| XSS | No input sanitization on product names, descriptions, review text |
| CSRF | Disabled everywhere (appropriate for stateless APIs) |
| Path traversal | Media service uses `entityType` in file path without sanitization |
| File upload security | Content-Type header checked (trivially spoofable); no magic byte validation |
| Rate limiting | NOT IMPLEMENTED -- brute-force attacks on login/register/forgot-password |
| Password policy | Only min length 8; no complexity requirements, no breach database check |
| Session fixation | N/A (stateless JWT) |
| Information disclosure | Stack traces not exposed, but error messages reveal email existence in debug logs |

---

## 6. Observability

### 6.1 Centralized Logging

**NOT IMPLEMENTED.**

| Concern | Status |
|---|---|
| Log aggregation | No ELK stack, no Loki, no CloudWatch integration |
| Structured logging | No JSON logging format; default Spring Boot text format |
| Log correlation | No trace ID in logs; cannot correlate requests across services |
| Log levels | Not dynamically configurable (no actuator loggers endpoint exposed) |
| Audit logging | Admin-service has audit entities but they are manually created, not auto-generated |

### 6.2 Metrics

| Concern | Status |
|---|---|
| Prometheus | Micrometer Prometheus dependency present in some services but not all |
| Custom business metrics | NOT IMPLEMENTED -- no order count, payment success rate, inventory levels |
| JVM metrics | Default Spring Boot Actuator metrics only |
| Kafka metrics | NOT EXPOSED |
| Database metrics | NOT EXPOSED |

### 6.3 Distributed Tracing

**NOT IMPLEMENTED.**

| Concern | Status |
|---|---|
| Micrometer Tracing | NOT INCLUDED in any pom.xml |
| Jaeger/Zipkin | NOT DEPLOYED |
| Trace ID propagation | No trace headers forwarded between services |
| OpenTelemetry | NOT INTEGRATED |

### 6.4 Visualization & Alerting

| Concern | Status |
|---|---|
| Grafana | NOT DEPLOYED |
| Alerting rules | NOT CONFIGURED |
| Dashboards | NOT CREATED |
| SLO/SLI monitoring | NOT DEFINED |

### 6.5 Health Checks

**Current State: PRESENT BUT INCOMPLETE**

| Concern | Status |
|---|---|
| Spring Boot Actuator | Configured in all services |
| Custom health indicators | NOT IMPLEMENTED -- no Kafka health check, no Redis health check, no DB health check beyond default |
| Payment service healthcheck | **WRONG PORT** -- Dockerfile checks port 8092, service runs on 8093 |
| Composite health | NOT IMPLEMENTED -- no readiness/liveness probes |

---

## 7. DevOps & Infrastructure

### 7.1 Containerization

| Concern | Status |
|---|---|
| Dockerfiles | Present for all 22 services |
| Multi-stage builds | NOT USED -- JARs must be pre-built |
| Non-root user | Only in some services (api-gateway, catalog-service have it; payment-service, cart-service run as root) |
| Image optimization | Alpine-based (good), but no multi-stage build to reduce image size |
| .dockerignore | NOT PRESENT |
| Image scanning | NOT CONFIGURED |

### 7.2 Orchestration

| Concern | Status |
|---|---|
| Docker Compose | Present (docker-compose.yml, docker-compose-minimal.yml, docker-compose.search.yml) |
| Kubernetes | **NO MANIFESTS OR HELM CHARTS** |
| Resource limits | NOT DEFINED in docker-compose |
| Network segmentation | SINGLE flat bridge network -- all 30+ containers on same network |
| Port conflicts | media-db/checkout-db on 5435; admin-service/review-service/recommendation-service on 8089; media-service/batch-service on 8091 |

### 7.3 CI/CD

**NOT IMPLEMENTED.**

| Concern | Status |
|---|---|
| GitHub Actions | No `.github/` directory |
| GitLab CI | No `.gitlab-ci.yml` |
| Jenkins | No Jenkinsfile |
| Automated builds | None |
| Automated tests | None |
| Container registry | Not configured |
| Deployment automation | None |
| Blue-green/canary | Not planned |
| Infrastructure as Code | No Terraform, no Pulumi, no CloudFormation |

### 7.4 Infrastructure at Scale

**Current docker-compose.yml issues for production:**

| Issue | Impact |
|---|---|
| 13 PostgreSQL instances | Each ~200MB RAM = 2.6GB just for Postgres |
| 6 MongoDB instances | Each ~500MB RAM = 3GB just for MongoDB |
| 18 Java services | Each ~256-512MB = 4.6-9.2GB for JVMs |
| Kafka + Redis + Eureka | ~1GB |
| **Total estimated minimum RAM** | **~12-16 GB** for development only |
| **No resource limits** | Any service can consume all host memory |
| **kafka-data volume declared but not mounted** | Kafka data lost on restart |

---

## 8. API Design

### 8.1 Versioning Strategy

| Concern | Status |
|---|---|
| URL versioning | Inconsistent: catalog uses `/api/v1/`, order uses `/order`, payment uses `/payment` |
| Header versioning | NOT IMPLEMENTED |
| Backward compatibility | No schema registry, no contract testing |

### 8.2 Rate Limiting & Throttling

**NOT IMPLEMENTED.**

| Endpoint | Risk |
|---|---|
| `POST /auth/login` | Brute-force password attacks |
| `POST /auth/register` | Mass account creation |
| `POST /auth/forgot-password` | Email flooding |
| `POST /payment/process` | Duplicate payment attempts |
| `GET /order` (unpaginated) | Database scan attack |
| All catalog endpoints | Scraping/price monitoring bots |

### 8.3 Backward Compatibility

| Concern | Status |
|---|---|
| Schema evolution | No Avro/Protobuf; raw Map/JSON events cannot evolve safely |
| API deprecation | No deprecation headers or sunset policy |
| Consumer contract testing | NOT IMPLEMENTED |

---

## 9. Testing Strategy

### 9.1 Current State: ZERO TESTS

**Not a single test file exists across all 22 services.**

| Test Type | Status |
|---|---|
| Unit tests | NOT PRESENT |
| Integration tests | NOT PRESENT |
| Contract tests (Pact) | NOT PRESENT |
| Load tests | NOT PRESENT |
| Chaos tests | NOT PRESENT |
| End-to-end tests | 26 shell scripts exist but they are smoke tests, not proper E2E tests |

### 9.2 Test Gaps by Risk Area

| Area | Test Needed |
|---|---|
| Inventory race conditions | Concurrent reservation tests with distributed locks |
| Payment idempotency | Duplicate payment request tests |
| Cart concurrency | Parallel cart modification tests |
| Coupon race condition | Concurrent applyCoupon tests |
| Kafka event ordering | Consumer ordering and deduplication tests |
| Auth JWT validation | Token generation, validation, expiration, revocation tests |
| API Gateway routing | Route matching, auth filter, stripPrefix tests |
| Saga compensation | Partial failure and rollback tests |

---

## 10. Grocery Domain Completeness

### 10.1 Required Services Assessment

| Domain Area | Service | Status | Gap |
|---|---|---|---|
| Product Catalog | catalog-service | Exists | Price field lost; no CDN; no caching |
| Inventory Management | inventory-service | Exists | Race conditions; no real-time WebSocket updates |
| Shopping Cart | cart-service | Exists | Hardcoded prices; race conditions; no guest-to-user merge |
| Order Management | order-service | Exists | No saga; no state machine; no pagination |
| Payment | payment-service | Exists (stub) | No real gateway; no idempotency; `create-drop` DDL |
| Delivery/Shipping | **MISSING** | Not implemented | No logistics, no tracking, no delivery time estimation |
| User & Auth | auth-service + user-service | Exists | Dual JWT bug; filter not wired; no social login |
| Notifications | notification-service | Exists (non-functional) | No Kafka consumers; no email/SMS integration |
| Search | search-service | Exists | No scoring; empty reindex; no typo tolerance |
| Pricing | pricing-service | Exists | No cache; Kafka no-op |
| Coupons | coupon-service | Exists | Race condition on usage count |
| Reviews | review-service | Exists | No Kafka events; no verified purchase check |
| Returns | returns-service | Exists | No payment integration; no order verification |
| Fraud Detection | fraud-service | Exists | Hardcoded rules; `create-drop` DDL |
| Analytics | analytics-service | Exists | No pagination; event loss |
| Recommendations | recommendation-service | Exists (non-functional) | No algorithm; no Kafka consumers |
| Media/Images | media-service | Exists | Local disk only; no CDN; no streaming endpoint |
| Admin Dashboard | admin-service | Exists (severely limited) | Dashboard zeros; audit logs unprotected |
| Batch Processing | batch-service | Exists (stub) | All jobs are sleep stubs |

### 10.2 Missing Grocery-Specific Features

| Feature | Status | Impact |
|---|---|---|
| Real-time inventory for perishables | NOT IMPLEMENTED | Cannot handle grocery-specific expiry/stock rotation |
| Delivery slot management | NOT IMPLEMENTED | Critical for grocery -- customers must choose delivery windows |
| Substitution preferences | NOT IMPLEMENTED | Out-of-stock product substitution is a core grocery feature |
| Weight-based pricing | NOT IMPLEMENTED | Many grocery items sold by weight, not unit |
| Expiry date tracking | NOT IMPLEMENTED | Essential for grocery inventory |
| Cold chain logistics | NOT IMPLEMENTED | Temperature-controlled delivery tracking |
| Multi-store/inventory | NOT IMPLEMENTED | Single inventory; no store-level stock |
| Recipe/bundle support | NOT IMPLEMENTED | Grocery bundles and meal kits |

---

## Detailed Findings by Priority

### CRITICAL FINDINGS

#### C1: Auth-service -- JwtAuthenticationFilter Not Wired
- **File:** `auth-service/.../config/SecurityConfig.java`
- **Problem:** `JwtAuthenticationFilter` is a `@Component` but never added to the `SecurityFilterChain`. All "protected" endpoints accept unauthenticated requests.
- **Fix:** Add `.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)` to the security filter chain.

#### C2: Auth-service -- Dual Incompatible JWT Implementations
- **File:** `auth-service/.../service/impl/AuthServiceImpl.java` vs `shared/security/JwtTokenProvider.java`
- **Problem:** `AuthServiceImpl` generates tokens with claim `"role"` (String), while `JwtTokenProvider` expects `"roles"` (List<String>). Tokens generated by the auth service will fail validation in any other service using the shared filter.
- **Fix:** Unify to a single JWT implementation. Modify `AuthServiceImpl` to use `JwtTokenProvider` for token generation.

#### C3: No Rate Limiting on API Gateway
- **File:** `api-gateway/` (all configs)
- **Problem:** Gateway has no `RequestRateLimiter` filter. During traffic spikes, backend services will be overwhelmed.
- **Fix:** Add `spring-cloud-starter-gateway` rate limiter with Redis-backed token bucket:
  ```yaml
  spring:
    cloud:
      gateway:
        routes:
          - id: auth-service
            uri: lb://auth-service
            filters:
              - name: RequestRateLimiter
                args:
                  redis-rate-limiter.replenishRate: 10
                  redis-rate-limiter.burstCapacity: 20
  ```

#### C4: Inventory Race Condition on REST Endpoints
- **File:** `inventory-service/.../service/impl/InventoryServiceImpl.java`
- **Problem:** `reserveInventory()`, `releaseReservation()`, and `adjustInventory()` have no concurrency protection. Only the Kafka-listener method `reserveInventoryForOrder()` uses the distributed lock.
- **Fix:** Apply `DistributedLockUtil.executeWithLock()` to all mutation endpoints, or add `@Version` optimistic locking to `InventoryItem`.

#### C5: Cart Hardcoded Prices + Race Condition
- **File:** `cart-service/.../service/impl/CartServiceImpl.java:118`
- **Problem:** New cart items get `BigDecimal.valueOf(99.99)` as price. `getOrCreateCart()` has a check-then-act race condition.
- **Fix:** Integrate with pricing-service via Feign client for real prices. Use Redis `SETNX` for atomic cart creation.

#### C6: Payment No Idempotency + Stub Gateway + `create-drop`
- **File:** `payment-service/.../service/impl/PaymentServiceImpl.java`, `application.yml`
- **Problem:** `Math.random()` for payment processing. Duplicate payment requests create duplicate charges. `ddl-auto: create-drop` destroys all data on restart.
- **Fix:** Implement idempotency key check, integrate Stripe/Razorpay SDK, change to `ddl-auto: validate`, enable Flyway.

#### C7: Checkout Missing Kafka + Inventory + Real Auth
- **File:** `checkout-service/` (all files)
- **Problem:** `extractUserIdFromToken()` returns hardcoded `"authenticated-user"`. No Kafka events published. No inventory reservation call.
- **Fix:** Implement real JWT parsing, add Kafka producer, add Feign client to inventory-service.

#### C8: Coupon `usedCount` Race Condition
- **File:** `coupon-service/.../service/impl/CouponServiceImpl.java`
- **Problem:** `usedCount` increment is read-modify-write without atomicity. Under concurrent access, usage limits are exceeded.
- **Fix:** Use atomic SQL: `UPDATE coupons SET used_count = used_count + 1 WHERE id = ? AND used_count < usage_limit` and check affected rows.

#### C9: Notification Service Non-Functional
- **File:** `notification-service/` (all files)
- **Problem:** No Kafka consumers. No email/SMS/push sending implementation. `ddl-auto: create-drop` loses all data.
- **Fix:** Add `@KafkaListener` for order/payment events. Integrate AWS SES/SendGrid/Twilio. Change to `ddl-auto: validate`.

#### C10: Batch Service All Stubs
- **File:** `batch-service/.../service/impl/BatchJobServiceImpl.java`
- **Problem:** All scheduled jobs are `Thread.sleep(1000)` with hardcoded counts. Spring Batch dependency exists but unused.
- **Fix:** Implement actual job logic (report generation, stock sync, price sync). Add `@EnableScheduling` and `@EnableAsync`.

---

### HIGH FINDINGS

#### H1: No Inter-Service Authentication
18 of 22 services permit all requests (`anyRequest().permitAll()`). If the API Gateway is bypassed, any service can access any other service's data. Implement mTLS or JWT-based service auth.

#### H2: No Circuit Breakers
No Resilience4j in any service. If auth-service goes down, all protected routes fail. If Kafka goes down, all events are lost. Add `@CircuitBreaker`, `@Retry`, `@Bulkhead` annotations.

#### H3: Kafka Events Silently Lost Everywhere
All Kafka producers use fire-and-forget with swallowed exceptions. No `acks=all`, no retries, no DLQ. Configure producer with:
```yaml
spring:
  kafka:
    producer:
      acks: all
      retries: 3
      properties:
        delivery.timeout.ms: 30000
        max.block.ms: 5000
```

#### H4: Unpaginated Endpoints (Order, Payment, Analytics)
`GET /order`, `GET /payment`, `getEventsByType` return all records. At millions of users, these will cause OOM crashes. Add `Pageable` support to all list endpoints.

#### H5: Catalog Price Field Lost in Mapping
`CreateProductRequest` has `price` field, `ProductResponse` has `price` field, but `Product` entity has NO `price` field. The price is validated, accepted, and silently discarded. Add `price` field to `Product` entity or move to `pricing-service` boundary.

#### H6: No CORS Configuration on Gateway
Browser-based frontends cannot reach the gateway. Add `CorsWebFilter` bean.

#### H7: `ddl-auto: update` or `create-drop` in Production
Services with risky DDL auto: auth (update), checkout (update), fraud (create-drop), notification (create-drop), admin (update), media (update), payment (create-drop). Change all to `validate` and enable Flyway.

#### H8: No Test Dependencies in Most Services
Even if tests existed, most services lack `spring-boot-starter-test` in pom.xml.

#### H9: DistributedLockUtil Non-Atomic Release
`releaseLock()` uses get-then-delete pattern in Redis, which is not atomic. Use Lua script:
```lua
if redis.call("get", KEYS[1]) == ARGV[1] then
  return redis.call("del", KEYS[1])
else
  return 0
end
```

#### H10: Analytics `enable-auto-commit: true`
Can lose events on crash between commit and save. Change to manual commit after successful persistence.

---

### MEDIUM FINDINGS

#### M1: Inconsistent API Versioning
Catalog uses `/api/v1/`, order uses `/order`, payment uses `/payment`. Standardize on `/api/v1/{service}/`.

#### M2: No Distributed Tracing
No Micrometer Tracing, no Jaeger/Zipkin. Cannot trace requests across services.

#### M3: Dead Code and Unused Dependencies
- `cart-service`: gRPC starter, Guava, Feign config (all unused)
- `catalog-service`: `ProductVariant` entity and repository (unused), `TestController` in production code
- `payment-service`: `PaymentRequest` DTO (unused), `DatabaseConfig` (empty)
- `order-service`: `DatabaseConfig` (empty)
- `auth-service`: `PasswordHasher` (unused), `OAuth2ClientConfig` (empty), `KeyRotationService` (no implementation)
- `batch-service`: Spring Batch and Quartz dependencies (never used)

#### M4: Kafka Events Use Untyped Maps
Events use `Map<String, Object>` or colon-delimited strings. Implement Avro schema registry or Protobuf contracts.

#### M5: No Health Check Dependencies
Actuator health endpoints don't check Kafka, Redis, or database connectivity beyond default indicators. Add custom `HealthIndicator` beans.

#### M6: Docker Port Conflicts
Port 5435 (media-db + checkout-db), 8089 (admin + review + recommendation), 8090 (returns + analytics), 8091 (media + batch), 8084 (order + search overlay).

#### M7: No Resource Limits in Docker Compose
No memory/CPU limits for any of 30+ containers.

#### M8: Catalog `TestController` in Production Code
`GET /api/v1/products` returns mock data from `TestController` alongside real data from `ProductController`. Bean ordering determines which wins.

#### M9: Inventory `updateStatus()` Checks Wrong Field
Checks `quantity` instead of `availableQuantity` (quantity - reservedQuantity). Items appear AVAILABLE when all stock is reserved.

#### M10: Payment Refund Ignores `refundAmount` and `reason`
Always performs full refund regardless of requested amount. No partial refund support.

---

### LOW FINDINGS

#### L1: WelcomeController on Every Service
Every service has a `GET /` returning "Welcome to X Service". Redundant with actuator; adds no value.

#### L2: Inconsistent Service ID Casing in Gateway
Mix of `lb://auth-service` and `lb://CATALOG-SERVICE`.

#### L3: No `@Version` Optimistic Locking on Most Entities
Order, Payment, Inventory, Coupon, and other entities lack `@Version` fields.

#### L4: Lombok `@Data` on JPA Entities
Generates `equals`/`hashCode` on all fields, causing issues with lazy loading and bidirectional relationships.

#### L5: LocalDateTime.now() in JPA Callbacks
Uses system clock; in distributed deployments, clock drift causes inconsistent timestamps.

#### L6: Empty 401 Responses from Gateway
No response body on authentication failures.

#### L7: No Soft Delete
Most services perform hard deletes. Soft delete with `deleted` flag is better for audit trails.

#### L8: No API Documentation
No OpenAPI/Swagger configuration in any service.

---

## Single Points of Failure

| Component | Risk | Mitigation |
|---|---|---|
| **Eureka (discovery-service)** | All 22 services depend on it; if down, no new registrations | Deploy 3-node Eureka cluster with peer awareness |
| **Single Kafka broker** | 1 partition per topic; if broker down, all events lost | Deploy 3-node Kafka cluster with replication factor 3 |
| **API Gateway (single instance)** | All external traffic flows through it | Deploy multiple gateway instances behind a load balancer |
| **Auth-service (single instance)** | Token validation bottleneck for all protected routes | Deploy multiple instances; implement JWT self-validation in gateway |
| **Individual PostgreSQL instances** | 13 separate instances; each a single point of failure | Use managed PostgreSQL with HA/failover |
| **Redis (single instance)** | Cart service, distributed locks, rate limiting all depend on it | Deploy Redis Sentinel or Redis Cluster |

---

## Anti-Patterns Identified

| Anti-Pattern | Location | Impact |
|---|---|---|
| **Shared code via copy-paste** | `shared/` package in auth-service and admin-service | Duplication; diverging implementations; not a proper shared library |
| **Fire-and-forget with silent failures** | Every Kafka producer across all services | Data loss; inconsistent state |
| **Synchronous calls in transaction boundaries** | Payment, Order, Inventory services | Long-running transactions; connection pool exhaustion |
| **No idempotency on write operations** | Order, Payment, Cart, Coupon services | Duplicate records; data corruption |
| **Hardcoded credentials in source/config** | All application.yml, docker-compose.yml | Security breach if repo is public |
| **Stub implementations in production code** | Payment (`Math.random()`), Batch (`Thread.sleep()`), Checkout (hardcoded user) | Non-functional features |
| **Dead dependencies** | gRPC in cart, Spring Batch in batch-service, Quartz in batch-service | Confusion; larger images; security scan noise |
| **Test code in production source** | `TestController` in catalog-service | Mock data in production |
| **Manual ObjectMapper per message** | Inventory event listener | CPU waste; inconsistent configuration |
| **Unbounded list returns** | Order, Payment, Analytics, Notification, Category | OOM at scale |
| **String-delimited Kafka messages** | Payment (`EVENT_TYPE:txId:orderId:timestamp`), Order (`EVENT_TYPE:number:timestamp`) | Fragile parsing; no schema evolution |
| **`ddl-auto: update/create-drop` in production configs** | 7 services | Schema drift; data loss |

---

## Target State: Scalable Reference Architecture

```
                                   +-------------------+
                                   |   CDN / WAF        |
                                   |  (CloudFront)      |
                                   +---------+---------+
                                             |
                                   +---------v---------+
                                   |  Load Balancer      |
                                   |  (ALB / NLB)        |
                                   +---------+---------+
                                             |
                          +------------------+------------------+
                          |                  |                  |
               +----------v----+   +---------v------+  +-------v---------+
               | API Gateway    |   | API Gateway     |  | API Gateway     |
               | (Instance 1)   |   | (Instance 2)    |  | (Instance 3)    |
               +------+---------+   +------+---------+  +------+----------+
                      |                  |                   |
           +----------+------------------+-------------------+----------+
           |          |                  |                   |          |
    +------v-----+ +--v------+ +---------v--+ +--------------v+  +-----v-----+
    | Auth        | | Catalog  | |  Order     | |   Payment      |  |  Cart      |
    | Service     | | Service  | |  Service   | |   Service      |  |  Service   |
    | [x3]        | | [x3]     | |  [x3]      | |   [x3]         |  |  [x3]      |
    +-----+------+ +-----+----+ +-----+------+ +------+---------+  +--+--+------+
          |               |            |               |              |    |
          |               |            |               |              |    |
    +-----v------+   +----v----+  +----v------+    +---v------+   +--v----v---+
    |PostgreSQL    |   |MongoDB  |  |PostgreSQL  |    |PostgreSQL |   |  Redis    |
    | (HA)         |   |(Atlas)  |  |  (HA)      |    |  (HA)     |   | Cluster   |
    +--------------+   +---------+  +------------+    +-----------+   +-----------+

    +-------------------------------------------------------------------------+
    |                        Apache Kafka Cluster (3 nodes)                    |
    |  Topics: user.*, product.*, order.*, payment.*, inventory.*,            |
    |          checkout.*, notification.*, analytics.*, pricing.*             |
    |  Schema Registry (Avro) | DLQ for each consumer group                   |
    +-------------------------------------------------------------------------+

    +------------------+  +------------------+  +---------------------+
    |  Prometheus       |  |  Grafana          |  |  Jaeger / Zipkin    |
    |  (Metrics)        |  |  (Dashboards)     |  |  (Distributed Tr.)  |
    +------------------+  +------------------+  +---------------------+

    +------------------+  +------------------+  +---------------------+
    |  ELK / Loki       |  |  HashiCorp Vault |  |  Kubernetes          |
    |  (Logging)        |  |  (Secrets)       |  |  (Orchestration)     |
    +------------------+  +------------------+  +---------------------+
```

**Key architectural changes from current state:**

1. **CDN + WAF** in front of gateway for DDoS protection and static asset caching
2. **Load balancer** distributing traffic across multiple gateway instances
3. **Multiple instances** of each service (minimum 3 for critical services)
4. **HA PostgreSQL** with read replicas for each service's database
5. **MongoDB Atlas** or managed MongoDB with replica sets
6. **Redis Cluster** for cart, caching, distributed locks, and rate limiting
7. **3-node Kafka cluster** with Schema Registry and DLQs
8. **Full observability stack**: Prometheus, Grafana, Jaeger, ELK/Loki
9. **HashiCorp Vault** for secrets management
10. **Kubernetes** for orchestration with HPA/VPA, network policies, and pod disruption budgets

---

## Step-by-Step Roadmap to Production Readiness

### Phase 1: Foundation & Security (Weeks 1-4) -- MUST DO

| # | Task | Effort | Priority |
|---|---|---|---|
| 1.1 | Fix auth-service: wire JWT filter, unify JWT implementations | 2 days | CRITICAL |
| 1.2 | Replace all hardcoded secrets with environment variables + Vault | 3 days | CRITICAL |
| 1.3 | Add API Gateway CORS configuration | 1 day | HIGH |
| 1.4 | Add rate limiting on API Gateway (Redis-backed) | 2 days | CRITICAL |
| 1.5 | Add authentication to all services (not just gateway) | 5 days | CRITICAL |
| 1.6 | Change all `ddl-auto: update/create-drop` to `validate` + enable Flyway | 3 days | CRITICAL |
| 1.7 | Fix Dockerfile port mismatch (payment-service healthcheck) | 1 day | HIGH |
| 1.8 | Add non-root user to all Dockerfiles | 1 day | HIGH |
| 1.9 | Add JVM memory limits (`-XX:MaxRAMPercentage=75.0`) to all Dockerfiles | 1 day | HIGH |
| 1.10 | Resolve port conflicts in docker-compose | 1 day | MEDIUM |

### Phase 2: Data Integrity & Concurrency (Weeks 5-8) -- MUST DO

| # | Task | Effort | Priority |
|---|---|---|---|
| 2.1 | Fix cart service: integrate with pricing-service, fix race condition | 3 days | CRITICAL |
| 2.2 | Fix inventory service: add concurrency protection to ALL mutation endpoints | 3 days | CRITICAL |
| 2.3 | Implement payment idempotency with idempotency keys | 3 days | CRITICAL |
| 2.4 | Fix coupon service: atomic usedCount increment | 2 days | CRITICAL |
| 2.5 | Add pagination to all list endpoints (order, payment, analytics, notification, category) | 4 days | HIGH |
| 2.6 | Implement SAGA pattern for order creation (orchestration-based) | 5 days | CRITICAL |
| 2.7 | Add Kafka outbox pattern for DB+event consistency | 5 days | HIGH |
| 2.8 | Implement real JWT extraction in checkout-service | 2 days | HIGH |
| 2.9 | Add distributed tracing (Micrometer Tracing + Jaeger) | 3 days | HIGH |
| 2.10 | Fix catalog-service price field mapping or move to pricing boundary | 2 days | HIGH |

### Phase 3: Resilience & Observability (Weeks 9-12)

| # | Task | Effort | Priority |
|---|---|---|---|
| 3.1 | Add Resilience4j circuit breakers to all inter-service calls | 5 days | CRITICAL |
| 3.2 | Configure Kafka producer: acks=all, retries, delivery timeout | 2 days | HIGH |
| 3.3 | Implement dead-letter topics and retry consumers | 4 days | HIGH |
| 3.4 | Add centralized logging (ELK or Loki) with JSON format | 4 days | HIGH |
| 3.5 | Deploy Prometheus + Grafana with custom business metrics | 4 days | HIGH |
| 3.6 | Implement JWT self-validation in gateway (eliminate per-request auth-service call) | 3 days | HIGH |
| 3.7 | Add custom health indicators (Kafka, Redis, DB) to all services | 3 days | MEDIUM |
| 3.8 | Implement notification-service Kafka consumers + email/SMS integration | 5 days | HIGH |
| 3.9 | Replace stub payment gateway with real integration (Stripe/Razorpay) | 5 days | CRITICAL |
| 3.10 | Implement actual batch service jobs | 5 days | MEDIUM |

### Phase 4: Infrastructure & DevOps (Weeks 13-16)

| # | Task | Effort | Priority |
|---|---|---|---|
| 4.1 | Set up CI/CD pipeline (GitHub Actions) | 5 days | CRITICAL |
| 4.2 | Implement comprehensive test suite (unit + integration + contract) | 10 days | CRITICAL |
| 4.3 | Create Kubernetes manifests / Helm charts | 7 days | HIGH |
| 4.4 | Implement HPA/VPA configs for autoscaling | 3 days | HIGH |
| 4.5 | Set up container registry and image scanning | 2 days | HIGH |
| 4.6 | Implement blue-green deployment strategy | 3 days | MEDIUM |
| 4.7 | Set up managed databases (RDS/Cloud SQL) with read replicas | 5 days | HIGH |
| 4.8 | Deploy Kafka cluster (managed: MSK/Confluent) | 3 days | HIGH |
| 4.9 | Set up Redis Cluster/Sentinel | 2 days | HIGH |
| 4.10 | Deploy Eureka cluster (3 nodes) or migrate to K8s service discovery | 3 days | MEDIUM |

### Phase 5: Advanced & Scale (Weeks 17-20)

| # | Task | Effort | Priority |
|---|---|---|---|
| 5.1 | Implement Redis caching for catalog, pricing, user services | 5 days | HIGH |
| 5.2 | Add API schema registry (Avro/Protobuf) for Kafka events | 4 days | HIGH |
| 5.3 | Implement CDN for media/images | 2 days | MEDIUM |
| 5.4 | Add load testing suite (k6/Gatling) with Big-Billion-Day scenarios | 4 days | HIGH |
| 5.5 | Implement chaos engineering tests | 3 days | MEDIUM |
| 5.6 | Build missing services: shipping/delivery, config-server | 8 days | MEDIUM |
| 5.7 | Implement grocery-specific features: delivery slots, substitutions, expiry tracking | 10 days | MEDIUM |
| 5.8 | Set up alerting rules and SLO monitoring | 3 days | HIGH |
| 5.9 | Implement mTLS for service-to-service communication | 4 days | HIGH |
| 5.10 | Performance tuning: connection pools, thread pools, GC tuning | 5 days | HIGH |

---

## Appendix A: Service Dependency Matrix

| Service | Depends On (sync) | Depends On (async) | Produces Events |
|---|---|---|---|
| auth-service | Redis (token revocation) | Kafka | user.created, user.updated |
| user-service | PostgreSQL | Kafka (user.created) | user.profile.updated |
| catalog-service | MongoDB | Kafka | product.created, product.updated |
| inventory-service | PostgreSQL, Redis (locks) | Kafka (order.placed, order.cancelled) | inventory.reserved, inventory.released |
| cart-service | Redis | -- | -- (should produce cart.updated) |
| checkout-service | PostgreSQL, cart-service, payment-service | -- | -- (should produce checkout.*) |
| order-service | PostgreSQL | Kafka | order-events (ORDER_CREATED, etc.) |
| payment-service | PostgreSQL | Kafka | payment-events |
| pricing-service | PostgreSQL | Kafka (product.created) | -- (should produce price.updated) |
| coupon-service | PostgreSQL | -- | -- (should produce coupon.*) |
| notification-service | PostgreSQL | -- (should consume order/payment events) | -- |
| search-service | MongoDB | Kafka (product.*) | -- |
| review-service | MongoDB | Kafka | -- (should produce review.*) |
| recommendation-service | MongoDB | -- (should consume user.behavior) | -- |
| returns-service | MongoDB | Kafka | return-events |
| fraud-service | PostgreSQL | Kafka (payment-events) | fraud.alert |
| analytics-service | MongoDB | Kafka (all events) | -- |
| media-service | PostgreSQL | Kafka | media.uploaded |
| admin-service | PostgreSQL | -- | -- |
| batch-service | -- | -- | -- |
| api-gateway | auth-service | -- | -- |
| discovery-service | -- | -- | -- |

---

## Appendix B: Kafka Topic Schema (Proposed)

| Topic | Key | Value Schema | Partitions | Retention |
|---|---|---|---|---|
| user.created | userId | Avro: UserCreatedEvent | 6 | 7 days |
| user.updated | userId | Avro: UserUpdatedEvent | 6 | 7 days |
| product.created | productId | Avro: ProductCreatedEvent | 6 | 30 days |
| product.updated | productId | Avro: ProductUpdatedEvent | 6 | 30 days |
| product.deleted | productId | Avro: ProductDeletedEvent | 3 | 7 days |
| order.placed | orderId | Avro: OrderPlacedEvent | 12 | 30 days |
| order.confirmed | orderId | Avro: OrderConfirmedEvent | 12 | 30 days |
| order.cancelled | orderId | Avro: OrderCancelledEvent | 6 | 30 days |
| payment.success | orderId | Avro: PaymentSuccessEvent | 12 | 90 days |
| payment.failed | orderId | Avro: PaymentFailedEvent | 6 | 30 days |
| inventory.reserved | sku | Avro: InventoryReservedEvent | 6 | 7 days |
| inventory.released | sku | Avro: InventoryReleasedEvent | 6 | 7 days |
| notification.send | userId | Avro: NotificationEvent | 6 | 7 days |
| checkout.started | orderId | Avro: CheckoutStartedEvent | 12 | 7 days |
| checkout.completed | orderId | Avro: CheckoutCompletedEvent | 12 | 30 days |
| DLQ.* | original key | Original event + error metadata | 3 | 90 days |

---

## Appendix C: Production Configuration Checklist

- [ ] All secrets in Vault, not in application.yml
- [ ] `ddl-auto: validate` on all services
- [ ] Flyway enabled on all services with production migrations
- [ ] JWT secret is 256+ bits, rotated quarterly
- [ ] BCrypt strength set to 12+
- [ ] Rate limiting configured on all public endpoints
- [ ] CORS configured with specific allowed origins
- [ ] Circuit breakers on all inter-service calls
- [ ] Kafka producer: acks=all, retries=3
- [ ] Kafka consumers: manual commit, DLQ configured
- [ ] Distributed tracing enabled
- [ ] Structured JSON logging with trace IDs
- [ ] Prometheus metrics exposed
- [ ] Grafana dashboards created
- [ ] Alerting rules configured
- [ ] Health checks include all dependencies
- [ ] Readiness/liveness probes configured
- [ ] Resource limits set (CPU, memory)
- [ ] HPA configured for autoscaling
- [ ] Network policies defined
- [ ] mTLS between services enabled
- [ ] Backup/restore tested for all databases
- [ ] CI/CD pipeline with automated tests
- [ ] Container image scanning enabled
- [ ] WAF/CDN in front of gateway
- [ ] Load testing completed for target traffic
- [ ] Runbook and on-call procedures documented

---

*Report generated by automated codebase analysis + principal architect review.*
*This system requires approximately 20 weeks of focused engineering effort to reach production readiness for millions of users.*
