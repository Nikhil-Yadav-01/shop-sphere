# ShopSphere - Payment Service

The **Payment Service** handles secure processing of customer payments. It integrates with external payment gateways (e.g., Stripe, PayPal), maintains transaction histories, processes webhooks, and handles refund orchestration.

## 🚀 Features
- **Payment Gateway Integration:** Secure tokenization and charging via SDKs.
- **Transaction Logging:** Immutable ledger of payment attempts and results.
- **Refund Processing:** Handles partial and full refunds.
- **Observability:** Spring Boot Actuator integration.

## 🛡️ Security Architecture
### Role-Based Access Control (RBAC)
- **CUSTOMER** (Initiate payments)
- **SYSTEM** (Internal coordination)
### Health Check Policy
- Spring Boot Actuator `/actuator/health`

## 🛠️ Tech Stack
- Java 21 / Spring Boot 3.2, PostgreSQL, Stripe/PayPal SDKs, Kafka

## 📂 API Endpoints
- `POST /api/v1/payments/charge` (Requires SYSTEM/CUSTOMER)
- `POST /api/v1/payments/webhook` (Public gateway webhook)

## ⚙️ Configuration
- `STRIPE_API_KEY`
- `SPRING_DATASOURCE_URL`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`