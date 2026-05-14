# ShopSphere - Batch Service

The **Batch Service** handles large-scale, scheduled background processing tasks. It executes end-of-day reconciliations, bulk catalog imports, and periodic report generation using Spring Batch, ensuring heavy operations do not impact user-facing APIs.

## 🚀 Features
- **Scheduled Job Execution:** Quartz integration for cron-based jobs.
- **Bulk Data Processing:** Import/Export operations for catalog and users.
- **End-of-day Reconciliation:** Financial and inventory aggregations.
- **Observability:** Spring Boot Actuator integration.

## 🛡️ Security Architecture
### Role-Based Access Control (RBAC)
- **SYSTEM, ADMIN**
### Health Check Policy
- Spring Boot Actuator `/actuator/health`

## 🛠️ Tech Stack
- Java 21 / Spring Boot 3.2, Spring Batch, PostgreSQL, Quartz Scheduler

## 📂 API Endpoints
- `POST /api/v1/batch/jobs/trigger` (Requires ADMIN)
- `GET /api/v1/batch/jobs/status` (Requires ADMIN)

## ⚙️ Configuration
- `SPRING_DATASOURCE_URL`
- `SPRING_BATCH_JOB_ENABLED`