# ShopSphere - Recommendation Service

The **Recommendation Service** provides personalized product suggestions to users. By analyzing browsing history, past purchases, and collaborative filtering algorithms, it enhances product discovery across the storefront.

## 🚀 Features
- **Personalized Recommendations:** Tailored suggestions per user.
- **Similar Products:** Item-to-item recommendations.
- **Trending Items:** Platform-wide popularity metrics.
- **Observability:** Spring Boot Actuator integration.

## 🛡️ Security Architecture
### Role-Based Access Control (RBAC)
- **CUSTOMER** (Retrieve personal recommendations)
- **SYSTEM** (Internal data ingestion)
### Health Check Policy
- Spring Boot Actuator `/actuator/health`

## 🛠️ Tech Stack
- Java 21 / Spring Boot 3.2, Neo4j / MongoDB, Kafka

## 📂 API Endpoints
- `GET /api/v1/recommendations/user/{id}` (Requires CUSTOMER)
- `GET /api/v1/recommendations/product/{id}/similar` (Public)

## ⚙️ Configuration
- `NEO4J_URI`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`