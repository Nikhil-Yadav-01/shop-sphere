# ShopSphere - Checkout Service

The **Checkout Service** orchestrates the complex checkout workflow. It coordinates with Cart, Inventory, Pricing, and Payment services using the Saga pattern to securely and reliably convert a shopping cart into a confirmed order.

## 🚀 Features
- **Checkout Orchestration:** Step-by-step state management for checkout.
- **Address Validation:** Ensures valid delivery destinations.
- **Distributed Transactions:** Saga pattern implementation across domains.
- **Observability:** Spring Boot Actuator integration.

## 🛡️ Security Architecture
### Role-Based Access Control (RBAC)
- **CUSTOMER**
### Health Check Policy
- Spring Boot Actuator `/actuator/health`

## 🛠️ Tech Stack
- Java 21 / Spring Boot 3.2, PostgreSQL, Kafka, Spring Cloud OpenFeign

## 📂 API Endpoints
- `POST /api/v1/checkout/initiate` (Requires CUSTOMER)
- `POST /api/v1/checkout/complete` (Requires CUSTOMER)

## ⚙️ Configuration
- `PAYMENT_SERVICE_URL`
- `ORDER_SERVICE_URL`
- `INVENTORY_SERVICE_URL`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`