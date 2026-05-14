# ShopSphere - User Service

The **User Service** manages customer profiles, shipping and billing addresses, and user preferences. It acts as the primary source of truth for user metadata, distinct from the core authentication credentials handled by Auth Service.

## 🚀 Features
- **Profile Management:** Name, contact details, and preferences.
- **Address Book:** Multiple shipping and billing addresses.
- **User Preferences:** Opt-ins for newsletters and notifications.
- **Observability:** Spring Boot Actuator integration.

## 🛡️ Security Architecture
### Role-Based Access Control (RBAC)
- **CUSTOMER** (Manage own profile)
- **ADMIN** (View/manage all users)
### Health Check Policy
- Spring Boot Actuator `/actuator/health`

## 🛠️ Tech Stack
- Java 21 / Spring Boot 3.2, PostgreSQL, Flyway

## 📂 API Endpoints
- `GET /api/v1/users/me` (Requires CUSTOMER)
- `PUT /api/v1/users/me/addresses` (Requires CUSTOMER)

## ⚙️ Configuration
- `SPRING_DATASOURCE_URL`
- `AUTH_SERVICE_URL`