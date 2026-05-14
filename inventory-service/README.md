# ShopSphere - Inventory Service

The **Inventory Service** tracks physical stock levels for all products across warehouses. It handles stock reservations during the checkout process and triggers restocking notifications when inventory runs low by publishing Kafka events.

## 🚀 Features
- **Stock Level Tracking:** Real-time visibility into product availability.
- **Inventory Reservation:** Temporary holds during checkout workflows.
- **Low Stock Alerts:** Event-driven notifications for procurement.
- **Observability:** Spring Boot Actuator integration.

## 🛡️ Security Architecture
### Role-Based Access Control (RBAC)
- **SYSTEM** (Reserve/Release operations)
- **ADMIN** (Stock management)
### Health Check Policy
- Spring Boot Actuator `/actuator/health`

## 🛠️ Tech Stack
- Java 21 / Spring Boot 3.2, PostgreSQL, Kafka

## 📂 API Endpoints
- `GET /api/v1/inventory/{sku}` (Public/System)
- `POST /api/v1/inventory/reserve` (Requires SYSTEM)

## ⚙️ Configuration
- `SPRING_DATASOURCE_URL`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`