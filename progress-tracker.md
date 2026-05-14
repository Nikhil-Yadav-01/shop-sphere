# Progress Tracker — ShopSphere Grocery

Tracked from: 2026-05-03

---

## Phase 1: Foundation & Security (Target: Week 4)

| # | Task | Status | Owner | Notes |
|---|---|---|---|---|
| 1.1 | Fix auth-service: wire JWT filter, unify JWT implementations | ✅ COMPLETED | AI | `JwtTokenProvider` now single JWT authority; removed defaults; filter wired; revocation checked |
| 1.2 | Replace all hardcoded secrets with env vars + Vault | ❌ NOT STARTED | | `jwt.secret` done in 1.1; db passwords, JWT secret in YAML, API keys remain |
| 1.3 | Add API Gateway CORS configuration | ❌ NOT STARTED | | |
| 1.4 | Add rate limiting on API Gateway (Redis-backed) | ❌ NOT STARTED | | |
| 1.5 | Add authentication enforcement to all services | ❌ NOT STARTED | | Most services still have `anyRequest().permitAll()` |
| 1.6 | Change `ddl-auto: update/create-drop` to `validate` + enable Flyway | ❌ NOT STARTED | | 7 services affected: auth, checkout, fraud, notification, admin, media, payment |
| 1.7 | Fix Dockerfile port mismatch (payment-service) | ❌ NOT STARTED | | health check hits port 8092, service runs on 8093 |
| 1.8 | Add non-root user to all Dockerfiles | ❌ NOT STARTED | | Only api-gateway and catalog-service have this |
| 1.9 | Add JVM memory flags to all Dockerfiles | ❌ NOT STARTED | | |
| 1.10 | Resolve port conflicts in docker-compose | ❌ NOT STARTED | | 5435, 8089, 8090, 8091 conflicts |

## Phase 2: Data Integrity & Concurrency (Target: Week 8)

| # | Task | Status | Owner | Notes |
|---|---|---|---|---|
| 2.1 | Fix cart service: integrate pricing, fix race condition | ❌ NOT STARTED | | |
| 2.2 | Fix inventory service: concurrency on all mutation endpoints | ❌ NOT STARTED | | |
| 2.3 | Implement payment idempotency | ❌ NOT STARTED | | |
| 2.4 | Fix coupon service: atomic usedCount increment | ❌ NOT STARTED | | |
| 2.5 | Add pagination to list endpoints | ❌ NOT STARTED | | Order, Payment, Analytics, Notification, Category |
| 2.6 | Implement SAGA pattern for order creation | ❌ NOT STARTED | | |
| 2.7 | Add Kafka outbox pattern | ❌ NOT STARTED | | |
| 2.8 | Implement real JWT extraction in checkout-service | ❌ NOT STARTED | | Currently hardcoded user ID |
| 2.9 | Add distributed tracing | ❌ NOT STARTED | | |
| 2.10 | Fix catalog price field mapping | ❌ NOT STARTED | | |

## Phase 3: Resilience & Observability (Target: Week 12)

| # | Task | Status | Owner | Notes |
|---|---|---|---|---|
| 3.1 | Add Resilience4j circuit breakers | ❌ NOT STARTED | | |
| 3.2 | Configure Kafka producers properly | ❌ NOT STARTED | | |
| 3.3 | Implement DLQ and retry consumers | ❌ NOT STARTED | | |
| 3.4 | Add centralized logging | ❌ NOT STARTED | | |
| 3.5 | Deploy Prometheus + Grafana | ❌ NOT STARTED | | |
| 3.6 | JWT self-validation in gateway | ❌ NOT STARTED | | |
| 3.7 | Custom health indicators | ❌ NOT STARTED | | |
| 3.8 | Notification service Kafka consumers | ❌ NOT STARTED | | |
| 3.9 | Real payment gateway integration | ❌ NOT STARTED | | |
| 3.10 | Real batch jobs | ❌ NOT STARTED | | |

## Phase 4: Infrastructure & DevOps (Target: Week 16)

| # | Task | Status | Owner | Notes |
|---|---|---|---|---|
| 4.1 | CI/CD pipeline | ❌ NOT STARTED | | |
| 4.2 | Comprehensive test suite | ❌ NOT STARTED | | |
| 4.3 | Kubernetes manifests / Helm charts | ❌ NOT STARTED | | |
| 4.4 | HPA/VPA autoscaling | ❌ NOT STARTED | | |
| 4.5 | Container registry + scanning | ❌ NOT STARTED | | |
| 4.6 | Blue-green deployment | ❌ NOT STARTED | | |
| 4.7 | Managed databases with read replicas | ❌ NOT STARTED | | |
| 4.8 | Managed Kafka cluster | ❌ NOT STARTED | | |
| 4.9 | Redis Cluster/Sentinel | ❌ NOT STARTED | | |
| 4.10 | Eureka cluster or K8s service discovery | ❌ NOT STARTED | | |

## Phase 5: Advanced & Scale (Target: Week 20)

| # | Task | Status | Owner | Notes |
|---|---|---|---|---|
| 5.1 | Redis caching for catalog, pricing, user | ❌ NOT STARTED | | |
| 5.2 | Schema registry (Avro/Protobuf) | ❌ NOT STARTED | | |
| 5.3 | CDN for media | ❌ NOT STARTED | | |
| 5.4 | Load testing suite | ❌ NOT STARTED | | |
| 5.5 | Chaos engineering | ❌ NOT STARTED | | |
| 5.6 | Missing services (shipping, config-server) | ❌ NOT STARTED | | |
| 5.7 | Grocery-specific features (delivery slots, etc.) | ❌ NOT STARTED | | |
| 5.8 | Alerting and SLO monitoring | ❌ NOT STARTED | | |
| 5.9 | mTLS for service-to-service | ❌ NOT STARTED | | |
| 5.10 | Performance tuning | ❌ NOT STARTED | | |

---

## Known Bugs & Quick Fixes

| Bug | Service | Priority | Status |
|---|---|---|---|
| catalog-service `TestController` has mock endpoint conflicting with real `ProductController` (`/api/v1/products`) | catalog | HIGH | OPEN |
| inventory `updateStatus()` checks `quantity` not `availableQuantity` | inventory | HIGH | OPEN |
| payment Dockerfile HEALTHCHECK uses wrong port (8092 vs 8093) | payment | HIGH | OPEN |
| `kafka-data` volume declared in docker-compose but not mounted | infra | MEDIUM | OPEN |
| Empty `DatabaseConfig.java` in order-service, payment-service | multiple | LOW | OPEN |
| `WelcomeController` on every service (redundant with actuator) | all | LOW | OPEN |

---

## Current Sprint Focus

**Phase 1 Task 1.1 — Auth service JWT fix**: ✅ COMPLETED
- `JwtTokenProvider`: single JWT authority, no default secret, email claim
- `AuthServiceImpl`: delegates all JWT to `JwtTokenProvider`, no inline JWT code
- `SecurityConfig`: `JwtAuthenticationFilter` wired into filter chain
- `JwtAuthenticationFilter`: token revocation check added

**Next:** Phase 1 Task 1.2 — Replace hardcoded secrets across all services
