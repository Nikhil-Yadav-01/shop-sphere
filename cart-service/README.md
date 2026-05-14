# ShopSphere - Cart Service

The **Cart Service** manages user shopping carts. It provides low-latency read and write operations for cart items, ensuring a seamless shopping experience before checkout. It leverages Redis for fast ephemeral storage and communicates with the Catalog Service via Feign clients to validate items.

## 🚀 Features
- **Add/Remove Cart Items:** Real-time cart state management.
- **Cart Total Calculation:** Live updates on cart values.
- **Cart Abandonment Tracking:** Emits events for abandoned carts.
- **Observability:** Spring Boot Actuator integration.

## 🛡️ Security Architecture
### Role-Based Access Control (RBAC)
- **CUSTOMER**
### Health Check Policy
- Spring Boot Actuator `/actuator/health`

## 🛠️ Tech Stack
- Java 21 / Spring Boot 3.2, Redis, Spring Cloud OpenFeign, Kafka

## 📂 API Endpoints
- `GET /api/v1/cart` (Requires CUSTOMER)
- `POST /api/v1/cart/items` (Requires CUSTOMER)
- `DELETE /api/v1/cart/items/{id}` (Requires CUSTOMER)

## ⚙️ Configuration
- `SPRING_REDIS_HOST`
- `CATALOG_SERVICE_URL`