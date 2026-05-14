# ShopSphere - Fraud Service

The **Fraud Service** evaluates user actions and transactions in real-time to detect anomalous behavior. It provides risk scores to the Checkout and Payment services to prevent fraudulent orders, consuming event streams via Kafka.

## 🚀 Features
- **Real-time Risk Scoring:** Machine-learning or rule-based score calculation.
- **Rule-based Fraud Detection:** Configurable thresholds (velocity, location).
- **Blocked User Management:** Integration with Auth Service for account lockouts.
- **Observability:** Spring Boot Actuator integration.

## 🛡️ Security Architecture
### Role-Based Access Control (RBAC)
- **SYSTEM, ADMIN**
### Health Check Policy
- Spring Boot Actuator `/actuator/health`

## 🛠️ Tech Stack
- Java 21 / Spring Boot 3.2, PostgreSQL, Kafka

## 📂 API Endpoints
- `POST /api/v1/fraud/evaluate` (Requires SYSTEM)
- `GET /api/v1/fraud/rules` (Requires ADMIN)

## ⚙️ Configuration
- `SPRING_DATASOURCE_URL`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`