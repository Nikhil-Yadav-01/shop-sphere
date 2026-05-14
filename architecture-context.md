# Architecture Context — ShopSphere Grocery

## System Overview

ShopSphere is a microservices-based grocery e-commerce platform designed for horizontal scalability. The system follows Domain-Driven Design with bounded contexts, event-driven communication via Apache Kafka, and API Gateway for external access.

---

## Architecture Diagram (Current)

```
Client (Mobile/Web)
       |
       v
+------------------+       +------------------+
|   API Gateway    | ----> |  Auth Service    |
|  (Spring Cloud   |       |  (port 8081)     |
|   Gateway)       |       +------------------+
|  (port 8080)     |               |
+--------+---------+               v
         |                 +------------------+
         |                 |  Eureka          |
         +---> /auth/**    |  Service Disc.   |
         +---> /catalog/** |  (port 8761)     |
         +---> /admin/**   +------------------+
         +---> /cart/**
         +---> /checkout/**
         +---> /order/**
         +---> /pricing/**
         +---> /batch/**
```

---

## Service Boundaries & Data Stores

| Service | Domain | Database | Port | Purpose |
|---|---|---|---|---|
| `auth-service` | Identity & Access | PostgreSQL | 8081 | Login, register, JWT, RBAC |
| `user-service` | User Profile | PostgreSQL | 8082 | Profile, addresses, preferences |
| `catalog-service` | Product Catalog | MongoDB | 8083 | Products, categories, variants |
| `order-service` | Order Management | PostgreSQL | 8084 | Order lifecycle |
| `cart-service` | Shopping Cart | Redis | 8085 | Cart CRUD |
| `checkout-service` | Checkout Flow | PostgreSQL | 8086 | Checkout orchestration |
| `pricing-service` | Pricing Engine | PostgreSQL | 8087 | Price calculation, rules |
| `coupon-service` | Discounts | PostgreSQL | 8088 | Coupon validation |
| `admin-service` | Admin Dashboard | PostgreSQL | 8089 | Admin operations, audit |
| `review-service` | Reviews | MongoDB | 8089 | Product reviews |
| `recommendation-service` | Recommendations | MongoDB | 8089 | ML-driven suggestions |
| `analytics-service` | Event Analytics | MongoDB | 8090 | User behavior analytics |
| `returns-service` | Returns | MongoDB | 8090 | Return/refund processing |
| `media-service` | File Storage | PostgreSQL | 8091 | Image/file uploads |
| `batch-service` | Scheduled Jobs | H2 | 8091 | Batch processing |
| `inventory-service` | Stock Management | PostgreSQL | 8092 | Inventory, reservations |
| `payment-service` | Payment Processing | PostgreSQL | 8093 | Payment gateway integration |
| `notification-service` | Notifications | PostgreSQL | 8095 | Email/SMS/push |
| `fraud-service` | Fraud Detection | PostgreSQL | 8010 | Fraud scoring |
| `search-service` | Product Search | MongoDB | 8098 | Full-text search |

---

## Infrastructure Services

| Service | Technology | Port | Purpose |
|---|---|---|---|
| `api-gateway` | Spring Cloud Gateway | 8080 | External entry point, auth, routing |
| `discovery-service` | Netflix Eureka | 8761 | Service registry |
| `kafka` | Apache Kafka (KRaft) | 9092 | Event bus |
| `redis` | Redis 7 | 6379 | Caching, cart, rate limiting, locks |

---

## Communication Patterns

### Synchronous (REST)
- **API Gateway -> Services**: External request routing
- **Gateway -> Auth Service**: Token validation (to be replaced with JWT self-validation)

### Asynchronous (Kafka)
- **Event-driven**: All inter-service business events
- **Current topics**: `order-events`, `payment-events`, `return-events`, `user.created`, `user.updated`, `product.created`, `product.updated`, `inventory.reserved`, `inventory.reservation.released`

### Missing Communication Patterns (Gaps)
- No gRPC implementations (proto files exist for cart but no service)
- No Feign clients between services (checkout has no client to inventory/payment/cart)
- No SAGA orchestrator for distributed transactions
- No outbox pattern for reliable event publication

---

## Current Architectural Issues

1. **Single JWT implementation gap**: Two incompatible implementations existed — fixed in Phase 1.1
2. **JWT filter was not wired**: Fixed in Phase 1.1
3. **No rate limiting**: Gateway has no protection against abuse
4. **No service-to-service auth**: Most services have no security (anyRequest().permitAll())
5. **Fire-and-forget Kafka**: No retries, no acks=all, no DLQ anywhere
6. **No circuit breakers**: Resilience4j not implemented
7. **Several critical stubs**: Payment uses Math.random(), Batch uses Thread.sleep()
8. **No pagination on list endpoints**: Order, Payment, Analytics return all records

---

## Target Architecture (Future State)

```
Client (Mobile/Web)
       |
       v
  [CDN / WAF]
       |
       v
  [Load Balancer]
       |
       v
+------------------+       +------------------+
|  API Gateway x3  | ----> |  Auth Service x3  |
|  + rate limiting |       |  + JWT self-val   |
+--------+---------+       +------------------+
         |                         |
         v                         v
   [Service Mesh / mTLS]     [Eureka Cluster]
         |
         v
  +-----+-----+-----+-----+
  |     |     |     |     |
  v     v     v     v     v
Catalog Order Cart   Payment ...
  |     |     |     |
  v     v     v     v
Mongo  PG    Redis  PG
 (HA)  (HA)  (Clus) (HA)
         |
         v
   [Kafka Cluster x3 + Schema Registry + DLQs]
         |
         v
  [Observability Stack]
  Prometheus + Grafana + Jaeger + ELK
```

---

## Key Architecture Decisions (ADRs)

### ADR-001: Event-Driven Communication
- **Decision**: Use Kafka for inter-service communication, not REST
- **Rationale**: Loose coupling, independent scaling, async processing
- **Status**: Partially implemented

### ADR-002: Database per Service
- **Decision**: Each service owns its data store
- **Rationale**: No shared databases, independent schema evolution
- **Status**: Implemented

### ADR-003: SAGA Pattern for Transactions
- **Decision**: Use choreography-based SAGA for distributed transactions
- **Rationale**: Avoid distributed transactions (2PC), eventual consistency
- **Status**: Not implemented (critical gap)

### ADR-004: JWT for Authentication
- **Decision**: Stateless JWT with access + refresh tokens
- **Rationale**: No session store needed, horizontal scaling
- **Status**: Partially implemented (single JWT authority now in place)
