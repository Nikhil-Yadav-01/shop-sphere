# ShopSphere - Analytics Service

The **Analytics Service** collects, aggregates, and analyzes telemetry and business metrics from across the ShopSphere ecosystem. It consumes events asynchronously via Kafka to provide insights into sales, user behavior, and system performance without impacting core transactional flows.

## 🚀 Features
- **Event Aggregation:** Collects real-time events across the platform.
- **Sales Reporting:** Generates historical and real-time sales reports.
- **User Behavior Tracking:** Analyzes customer journeys and drop-offs.
- **Observability:** Spring Boot Actuator integration.

## 🛡️ Security Architecture
### Role-Based Access Control (RBAC)
- **ADMIN, SYSTEM**
### Health Check Policy
- Spring Boot Actuator `/actuator/health`

## 🛠️ Tech Stack
- Java 21 / Spring Boot 3.2, MongoDB / ClickHouse, Kafka

## 📂 API Endpoints
- `GET /api/v1/analytics/sales` (Requires ADMIN)
- `GET /api/v1/analytics/traffic` (Requires ADMIN)

## ⚙️ Configuration
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`
- `ANALYTICS_DB_URL`