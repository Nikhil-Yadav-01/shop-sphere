# Search Service

A fully decoupled, production-ready search service for ShopSphere e-commerce platform. This service provides comprehensive search capabilities with support for full-text search, filtering, and advanced querying.

## Features

- **Full-Text Search**: MongoDB text-indexed search across product names and descriptions
- **Advanced Filtering**: Filter by category, status, price range, and stock availability
- **Kafka Integration**: Listens to product events for real-time index updates
- **Service Discovery**: Automatic registration with Eureka service discovery
- **Health Checks**: Built-in health monitoring and metrics
- **MongoDB Persistence**: Efficient document storage and indexing

## Endpoints

### Welcome & Health
- `GET /api/v1/welcome` - Welcome message
- `GET /api/v1/health-check` - Service health check
- `GET /actuator/health` - Spring Actuator health

### Search Operations
- `POST /api/v1/search?page=0&size=10` - Full search with filters
  ```json
  {
    "keyword": "laptop",
    "categoryId": "cat123",
    "status": "ACTIVE",
    "inStock": true,
    "minPrice": 100,
    "maxPrice": 5000
  }
  ```

- `GET /api/v1/search/keyword?keyword=laptop&page=0&size=10` - Search by keyword
- `GET /api/v1/search/category/{categoryId}?page=0&size=10` - Search by category
- `GET /api/v1/search/status/{status}?page=0&size=10` - Search by status
- `GET /api/v1/search/price?minPrice=100&maxPrice=5000&page=0&size=10` - Search by price range
- `GET /api/v1/search/in-stock/{inStock}?page=0&size=10` - Search by stock status
- `GET /api/v1/search/index-size` - Get total indexed products count
- `POST /api/v1/search/reindex` - Trigger reindexing (admin)

## Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Database**: MongoDB 7.0
- **Message Queue**: Apache Kafka
- **Service Discovery**: Netflix Eureka
- **Security**: Spring Security (stateless)
- **Build**: Maven

## Configuration

Required environment variables:
- `MONGO_URI` - MongoDB connection string (default: `mongodb://localhost:27017/shopsphere_search`)
- `EUREKA_HOST` - Eureka service host (default: `localhost`)
- `EUREKA_PORT` - Eureka service port (default: `8761`)
- `KAFKA_BOOTSTRAP_SERVERS` - Kafka broker addresses (default: `localhost:9092`)

## Kafka Topics

The service listens to:
- `product-created` - New product indexed
- `product-updated` - Product index updated
- `product-deleted` - Product index removed

## Running Locally

### Using Docker Compose
```bash
docker-compose -f docker-compose.search.yml up -d
```

### Manual Build
```bash
cd search-service
mvn clean package -DskipTests
docker build -f Dockerfile -t search-service:latest ..
```

## Architecture

```
SearchController
    ↓
SearchService (Interface)
    ↓
SearchServiceImpl
    ↓
SearchIndexRepository (MongoDB)
    ↓
SearchIndex Entity
```

### Event-Driven Updates
```
Catalog Service → Kafka Topics
                    ↓
              ProductEventConsumer
                    ↓
              SearchService (indexing)
                    ↓
              MongoDB Search Index
```

## Database Schema

**Collection**: `search_index`
- `_id`: MongoDB ObjectId
- `productId`: Unique product identifier
- `name`: Text indexed
- `description`: Text indexed
- `sku`: Indexed
- `price`: Numeric
- `categoryId`: Indexed
- `categoryName`: Indexed
- `tags`: Text indexed array
- `status`: Indexed
- `rating`: Numeric
- `reviewCount`: Numeric
- `inStock`: Boolean indexed
- `brand`: Text indexed
- `createdAt`: Auto-created timestamp
- `updatedAt`: Auto-modified timestamp
- `indexedAt`: Timestamp when indexed

## Performance Considerations

- MongoDB text indexes enable fast full-text search
- Pagination prevents large result sets
- Event-driven updates keep search index in sync
- Service discovery enables horizontal scaling
- Stateless design allows multiple instances

## Development

### Project Structure
```
search-service/
├── pom.xml
├── Dockerfile
├── src/main/
│   ├── java/
│   │   └── com/rudraksha/shopsphere/search/
│   │       ├── SearchApplication.java
│   │       ├── controller/
│   │       ├── service/
│   │       ├── repository/
│   │       ├── entity/
│   │       ├── dto/
│   │       ├── kafka/
│   │       └── config/
│   └── resources/
│       └── application.yml
└── README.md
```

## Deployment

The service is designed for:
- **Containerized deployment** (Docker/Kubernetes)
- **Cloud-native architecture**
- **Horizontal scaling** with load balancing
- **Service mesh integration** (Istio)

## Monitoring

The service provides:
- Spring Boot Actuator metrics at `/actuator`
- Health checks at `/actuator/health`
- Index size information via REST API
- Kafka consumer metrics
- MongoDB connection metrics

## Version

Version: 1.0.0
Java: 21
Spring Boot: 3.2.0
