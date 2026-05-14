# ShopSphere - Catalog Service

The **Catalog Service** is the source of truth for all product information, categories, and brands. It serves product details to the storefront and integrates with Search and Inventory services by emitting updates via Kafka.

## 🚀 Features
- **Product Management:** CRUD operations for grocery items.
- **Category & Brand Hierarchies:** Organized taxonomy for navigation.
- **Product Variants:** Support for different sizes and weights.
- **Observability:** Spring Boot Actuator integration.

## 🛡️ Security Architecture
### Role-Based Access Control (RBAC)
- **CUSTOMER** (Read operations)
- **SELLER, ADMIN** (Write operations)
### Health Check Policy
- Spring Boot Actuator `/actuator/health`

## 🛠️ Tech Stack
- Java 21 / Spring Boot 3.2, PostgreSQL, Flyway, Kafka

## 📂 API Endpoints
- `GET /api/v1/catalog/products` (Public/CUSTOMER)
- `POST /api/v1/catalog/products` (Requires ADMIN/SELLER)

## ⚙️ Configuration
- `SPRING_DATASOURCE_URL`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`