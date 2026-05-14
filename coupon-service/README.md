# ShopSphere - Coupon Service

The **Coupon Service** manages promotional codes, discounts, and voucher rules. It validates applied coupons during the checkout process and calculates discount amounts in coordination with the Pricing Service.

## 🚀 Features
- **Coupon Creation & Validation:** Define and verify promo rules.
- **Usage Tracking & Limits:** Enforce per-user and global usage limits.
- **Dynamic Discount Rules:** Percentage, fixed amount, and BOGO logic.
- **Observability:** Spring Boot Actuator integration.

## 🛡️ Security Architecture
### Role-Based Access Control (RBAC)
- **CUSTOMER** (Read/Apply operations)
- **ADMIN** (Write operations)
### Health Check Policy
- Spring Boot Actuator `/actuator/health`

## 🛠️ Tech Stack
- Java 21 / Spring Boot 3.2, PostgreSQL, Redis

## 📂 API Endpoints
- `POST /api/v1/coupons/validate` (Requires CUSTOMER)
- `POST /api/v1/coupons` (Requires ADMIN)

## ⚙️ Configuration
- `SPRING_DATASOURCE_URL`
- `SPRING_REDIS_HOST`