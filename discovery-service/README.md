# ShopSphere - Discovery Service

The **Discovery Service** is the central service registry for the ShopSphere ecosystem. Powered by Netflix Eureka, it allows microservices to discover and communicate with each other dynamically without relying on hardcoded IP addresses.

## 🚀 Features
- **Service Registration:** Automatic registration of booting microservices.
- **Health Monitoring:** Continuous heartbeat checks to evict stale nodes.
- **Dynamic Resolution:** Client-side load balancing via Spring Cloud LoadBalancer.
- **Observability:** Spring Boot Actuator integration.

## 🛡️ Security Architecture
### Role-Based Access Control (RBAC)
- **SYSTEM** (Internal network access only)
### Health Check Policy
- Spring Boot Actuator `/actuator/health`

## 🛠️ Tech Stack
- Java 21 / Spring Boot 3.2, Spring Cloud Netflix Eureka Server

## 📂 API Endpoints
- Eureka Dashboard UI (`/`)
- `GET /eureka/apps` (Internal Service List)

## ⚙️ Configuration
- `EUREKA_INSTANCE_HOSTNAME`
- `EUREKA_CLIENT_REGISTERWITH_EUREKA` (usually false for the server itself)