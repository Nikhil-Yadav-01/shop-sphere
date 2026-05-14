# Project Overview — ShopSphere Grocery

## Description

ShopSphere is a cloud-native grocery e-commerce backend built with microservices architecture. It is designed for horizontal scalability and eventual handling of millions of concurrent users. The system covers the full grocery e-commerce lifecycle: user management, product catalog, cart, checkout, order management, payment processing, inventory management, delivery logistics, and administrative operations.

---

## Tech Stack

### Core
- **Language**: Java 21
- **Framework**: Spring Boot 3.2.0
- **Cloud**: Spring Cloud 2023.0.0
- **Service Discovery**: Netflix Eureka
- **API Gateway**: Spring Cloud Gateway

### Data Stores
- **Primary Relational**: PostgreSQL 15 (13 instances, one per service)
- **Document Store**: MongoDB 7.0 (6 instances)
- **Cache**: Redis 7 Alpine (1 instance, used by cart-service)

### Messaging
- **Event Bus**: Apache Kafka (KRaft mode, single node)

### Security
- **Auth**: JWT (access + refresh tokens)
- **Password Hashing**: BCrypt
- **Token Revocation**: Redis

### Infrastructure
- **Containerization**: Docker (22 services, 20 DB instances)
- **Orchestration**: Docker Compose (dev/staging)
- **Database Migrations**: Flyway (PostgreSQL services)

---

## Service Inventory (22 Microservices)

### Core Business Services
| Service | Port | DB | Description |
|---|---|---|---|
| auth-service | 8081 | PostgreSQL | Authentication, JWT, RBAC |
| user-service | 8082 | PostgreSQL | User profiles, addresses |
| catalog-service | 8083 | MongoDB | Product catalog, categories |
| cart-service | 8085 | Redis | Shopping cart |
| checkout-service | 8086 | PostgreSQL | Checkout orchestration |
| order-service | 8084 | PostgreSQL | Order lifecycle |
| payment-service | 8093 | PostgreSQL | Payment processing |
| inventory-service | 8092 | PostgreSQL | Stock management |
| pricing-service | 8087 | PostgreSQL | Price calculation |
| coupon-service | 8088 | PostgreSQL | Discount codes |
| notification-service | 8095 | PostgreSQL | Email/SMS/push |

### Supporting Services
| Service | Port | DB | Description |
|---|---|---|---|
| search-service | 8098 | MongoDB | Product search |
| review-service | 8089 | MongoDB | Product reviews |
| recommendation-service | 8089 | MongoDB | Recommendations |
| returns-service | 8097 | MongoDB | Returns/refunds |
| fraud-service | 8010 | PostgreSQL | Fraud detection |
| analytics-service | 8090 | MongoDB | Event analytics |
| media-service | 8091 | PostgreSQL | File/image storage |
| admin-service | 8089 | PostgreSQL | Admin dashboard |

### Infrastructure Services
| Service | Port | Description |
|---|---|---|
| api-gateway | 8080 | API Gateway |
| discovery-service | 8761 | Eureka Registry |

### Batch Jobs
| Service | Port | DB | Description |
|---|---|---|---|
| batch-service | 8091 | H2 | Scheduled jobs (reports, sync) |

---

## Current State

### What Works (Core Functionality)
- User login/registration with JWT ✅ (recently fixed)
- Product catalog CRUD with MongoDB pagination ✅
- Cart CRUD with Redis persistence ✅
- Order creation with Kafka event publishing ✅
- Payment creation with simulated gateway ✅
- Inventory basic operations ✅
- Service discovery via Eureka ✅
- API Gateway routing with JWT validation ✅
- All services containerized with Docker ✅

### What Does NOT Work (Production Gaps)
- **Zero test coverage** — no unit/integration/contract tests
- **No rate limiting** — API abuse protection missing
- **No inter-service auth** — most services are publicly accessible
- **No circuit breakers** — cascade failure risk
- **No SAGA pattern** — distributed transactions unchecked
- **No idempotency** — duplicate charges, orders, reservations
- **Fire-and-forget Kafka** — events silently lost
- **No observability** — no Prometheus, Grafana, Jaeger, ELK
- **No CI/CD** — no automated build/test/deploy
- **Race conditions** in inventory, cart, coupon services
- **Stub implementations** in payment, batch, notification services

---

## How to Run

### Prerequisites
- Java 21+
- Docker Desktop + Docker Compose
- Maven 3.8+

### Full Stack (Docker Compose)
```bash
# Build all services
mvn clean package -DskipTests

# Start everything (22 services + 20 DB instances)
docker compose up -d

# Wait ~2 minutes for all services to register with Eureka
# Check: http://localhost:8761
```

### Minimal Dev Environment
```bash
# Start only infrastructure
docker compose -f docker-compose-minimal.yml up -d

# Run individual services via Maven with local profile
cd auth-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Access Points
- **API Gateway**: http://localhost:8080
- **Eureka Dashboard**: http://localhost:8761
- **Auth Service**: http://localhost:8081 (direct, not through gateway)

---

## Project Maturity

| Area | Score (1-10) | Notes |
|---|---|---|
| Architecture | 7 | Sound fundamentals, well-structured |
| Security | 3 | JWT fixed, but most services unprotected |
| Data Integrity | 2 | Race conditions, no idempotency, no SAGA |
| Scalability | 3 | No caching, no read replicas, no pagination |
| Resilience | 1 | No circuit breakers, retries, or timeouts |
| Observability | 1 | No centralized logging, tracing, metrics |
| Testing | 0 | No tests |
| DevOps | 2 | Dockerized but no CI/CD, no K8s |
| **Overall** | **2.4** | |
