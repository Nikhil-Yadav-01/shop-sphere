# ShopSphere - Review Service

The **Review Service** allows customers to leave ratings and text reviews for products they have purchased. It ensures social proof and includes moderation capabilities to flag or remove inappropriate content.

## 🚀 Features
- **Product Ratings & Reviews:** Aggregate scores and text feedback.
- **Review Moderation:** Tools for admins to hide reported reviews.
- **Verified Purchase Badges:** Cross-checks with Order Service.
- **Observability:** Spring Boot Actuator integration.

## 🛡️ Security Architecture
### Role-Based Access Control (RBAC)
- **CUSTOMER** (Submit/Edit own reviews)
- **ADMIN** (Moderate reviews)
### Health Check Policy
- Spring Boot Actuator `/actuator/health`

## 🛠️ Tech Stack
- Java 21 / Spring Boot 3.2, MongoDB, Kafka

## 📂 API Endpoints
- `GET /api/v1/reviews/product/{id}` (Public)
- `POST /api/v1/reviews` (Requires CUSTOMER)

## ⚙️ Configuration
- `SPRING_DATA_MONGODB_URI`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`