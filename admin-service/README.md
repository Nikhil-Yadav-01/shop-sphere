# ShopSphere - Admin Service

The **Admin Service** is the central administrative backend for ShopSphere. It provides APIs for platform administrators to manage users, monitor platform health, configure global settings, and oversee e-commerce operations. It communicates with other services via REST and Feign clients.

## 🚀 Features
- **User & Role Management:** Manage platform users and internal staff roles.
- **Platform Configuration:** Global settings and feature flags.
- **System Monitoring:** Aggregated health metrics and audit logs.
- **Observability:** Spring Boot Actuator integration.

## 🛡️ Security Architecture
### Role-Based Access Control (RBAC)
- **ADMIN, SUPER_ADMIN**
### Health Check Policy
- Spring Boot Actuator `/actuator/health`

## 🛠️ Tech Stack
- Java 21 / Spring Boot 3.2, PostgreSQL, Spring Cloud OpenFeign, Eureka Client

## 📂 API Endpoints
- `GET /api/v1/admin/users` (Requires ADMIN)
- `POST /api/v1/admin/settings` (Requires SUPER_ADMIN)

## ⚙️ Configuration
- `SPRING_DATASOURCE_URL`
- `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`
- `AUTH_SERVICE_URL`