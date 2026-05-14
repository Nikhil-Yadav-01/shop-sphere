# ShopSphere - Notification Service

The **Notification Service** is the central hub for outbound communications. It consumes domain events via Kafka to asynchronously send emails, SMS messages, and push notifications to users without blocking core business processes.

## 🚀 Features
- **Email & SMS Delivery:** Integration with third-party providers (SendGrid, Twilio).
- **Push Notifications:** Web and mobile alerts.
- **Template Management:** Centralized message templates.
- **Observability:** Spring Boot Actuator integration.

## 🛡️ Security Architecture
### Role-Based Access Control (RBAC)
- **SYSTEM** (Event-driven triggers)
- **ADMIN** (Manual broadcasts)
### Health Check Policy
- Spring Boot Actuator `/actuator/health`

## 🛠️ Tech Stack
- Java 21 / Spring Boot 3.2, Kafka, JavaMail / external APIs

## 📂 API Endpoints
- `POST /api/v1/notifications/send` (Requires ADMIN)
- Listens to Kafka topics: `order-events`, `user-events`

## ⚙️ Configuration
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`
- `MAIL_PROVIDER_API_KEY`