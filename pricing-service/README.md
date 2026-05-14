# ShopSphere - Pricing Service

The **Pricing Service** calculates real-time prices for products based on base prices, active promotions, customer tiers, and regional taxes. It is heavily queried during catalog browsing and checkout phases.

## 🚀 Features
- **Dynamic Price Calculation:** Evaluates complex pricing rules in milliseconds.
- **Tax Calculation Integration:** Regional tax application.
- **Tiered Pricing:** VIP and wholesale customer discounts.
- **Observability:** Spring Boot Actuator integration.

## 🛡️ Security Architecture
### Role-Based Access Control (RBAC)
- **SYSTEM, CUSTOMER**
### Health Check Policy
- Spring Boot Actuator `/actuator/health`

## 🛠️ Tech Stack
- Java 21 / Spring Boot 3.2, Redis, Spring Cloud OpenFeign

## 📂 API Endpoints
- `POST /api/v1/pricing/calculate` (Requires SYSTEM/CUSTOMER)
- `GET /api/v1/pricing/{sku}` (Public/CUSTOMER)

## ⚙️ Configuration
- `COUPON_SERVICE_URL`
- `CATALOG_SERVICE_URL`
- `SPRING_REDIS_HOST`