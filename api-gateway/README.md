# ShopSphere - API Gateway

The **API Gateway** is the main entry point for all external traffic entering the ShopSphere ecosystem. It provides dynamic routing, global rate limiting, and global security filtering (like JWT validation) before requests reach internal microservices.

## 🚀 Features
- **Dynamic Request Routing:** Routes incoming requests to appropriate backend services.
- **Global Rate Limiting:** Prevents abuse using Redis-backed rate limiters.
- **Authentication Token Validation:** Verifies JWT signatures before forwarding.
- **Observability:** Spring Boot Actuator integration.

## 🛡️ Security Architecture
### Role-Based Access Control (RBAC)
- **ALL** (Enforces presence of valid JWT, delegates specific RBAC to downstream services)
### Health Check Policy
- Spring Boot Actuator `/actuator/health`

## 🛠️ Tech Stack
- Java 21 / Spring Boot 3.2, Spring Cloud Gateway, Redis, Eureka Client

## 📂 API Endpoints
- Routes `/api/v1/auth/**` to Auth Service
- Routes `/api/v1/catalog/**` to Catalog Service

## ⚙️ Configuration
- `SPRING_REDIS_HOST`
- `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`
- `JWT_PUBLIC_KEY`