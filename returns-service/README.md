# ShopSphere - Returns Service

The **Returns Service** manages Return Merchandise Authorizations (RMAs). It handles customer requests to return items, evaluates return eligibility based on policies, and orchestrates the physical return and refund process.

## 🚀 Features
- **RMA Creation & Tracking:** Full lifecycle of return requests.
- **Return Eligibility Rules:** Validates time windows and product types.
- **Refund Orchestration:** Communicates with Payment Service upon return validation.
- **Observability:** Spring Boot Actuator integration.

## 🛡️ Security Architecture
### Role-Based Access Control (RBAC)
- **CUSTOMER** (Initiate/Track own returns)
- **ADMIN** (Process/Approve returns)
### Health Check Policy
- Spring Boot Actuator `/actuator/health`

## 🛠️ Tech Stack
- Java 21 / Spring Boot 3.2, PostgreSQL, Kafka, Spring Cloud OpenFeign

## 📂 API Endpoints
- `POST /api/v1/returns` (Requires CUSTOMER)
- `PUT /api/v1/returns/{id}/status` (Requires ADMIN)

## ⚙️ Configuration
- `SPRING_DATASOURCE_URL`
- `ORDER_SERVICE_URL`