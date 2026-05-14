# ShopSphere - Order Service

The **Order Service** manages the entire lifecycle of a customer order. From creation and payment confirmation to fulfillment and delivery tracking, it serves as the definitive source of truth for all order data.

## 🚀 Features
- **Order State Machine:** Transitions (PENDING, PAID, SHIPPED, DELIVERED).
- **Order History & Tracking:** Detailed views for customers.
- **Fulfillment Integration:** Broadcasts events to downstream logistics.
- **Observability:** Spring Boot Actuator integration.

## 🛡️ Security Architecture
### Role-Based Access Control (RBAC)
- **CUSTOMER** (View/manage own orders)
- **ADMIN** (View/manage all orders)
### Health Check Policy
- Spring Boot Actuator `/actuator/health`

## 🛠️ Tech Stack
- Java 21 / Spring Boot 3.2, PostgreSQL, Kafka, Spring Cloud OpenFeign

## 📂 API Endpoints
- `GET /api/v1/orders` (Requires CUSTOMER/ADMIN)
- `GET /api/v1/orders/{id}` (Requires CUSTOMER/ADMIN)

## ⚙️ Configuration
- `SPRING_DATASOURCE_URL`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`