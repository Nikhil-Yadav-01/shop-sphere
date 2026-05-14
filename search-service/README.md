# ShopSphere - Search Service

The **Search Service** provides fast, full-text, and faceted search capabilities for the product catalog. It continuously indexes product updates from Kafka into Elasticsearch to guarantee sub-second search queries.

## 🚀 Features
- **Full-text Search:** Keyword matching and relevance scoring.
- **Faceted Filtering:** Drill-down by price, brand, and category.
- **Search Suggestions:** Autocomplete features for the search bar.
- **Observability:** Spring Boot Actuator integration.

## 🛡️ Security Architecture
### Role-Based Access Control (RBAC)
- **CUSTOMER / PUBLIC** (Search queries)
- **SYSTEM** (Index updates)
### Health Check Policy
- Spring Boot Actuator `/actuator/health`

## 🛠️ Tech Stack
- Java 21 / Spring Boot 3.2, Elasticsearch, Kafka

## 📂 API Endpoints
- `GET /api/v1/search` (Public)
- `GET /api/v1/search/suggestions` (Public)

## ⚙️ Configuration
- `ELASTICSEARCH_URIS`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`