# ShopSphere - Auth Service

The **Auth Service** is the central security authority for the ShopSphere microservices ecosystem. It handles user registration, authentication (Local, Google, Apple), session management via JWT, and account security features like email verification and rate limiting.

## 🚀 Features

- **Multi-Provider Authentication:**
  - **Local:** Traditional email and password (BCrypt hashed).
  - **Google:** Secure ID Token verification using `google-api-client`.
  - **Apple:** Production-ready ES256 JWT verification with dynamic JWKS caching.
- **Token Management:**
  - Stateless JWT issuance for session management.
  - Refresh token rotation flow for long-lived sessions.
  - Token revocation (blacklist) stored in Redis for instant logout.
- **Account Security:**
  - **Email Verification:** Mandatory verification flow before login.
  - **Password Reset:** Secure token-based recovery via Kafka-driven email notifications.
  - **Brute Force Protection:** Automatic account lockout after 5 failed attempts (Redis-backed).
  - **Rate Limiting:** Request-level throttling for sensitive endpoints.
- **Observability:**
  - Full Spring Boot Actuator integration (Prometheus metrics, Health checks).
  - Restricted access to sensitive metrics (Admin role only).

## 🛡️ Security Architecture

### Role-Based Access Control (RBAC)
- **CUSTOMER:** Default role for new users.
- **SELLER:** Authorized to manage products and orders.
- **ADMIN:** Full system access, including sensitive `/actuator` data.

### Health Check Policy
- Public access is allowed ONLY to the root path (`/`) and basic health status.
- Detailed health metrics, environment info, and thread dumps under `/actuator/**` require an **Admin** token.

## 🛠️ Tech Stack

- **Java 21 / Spring Boot 3.2**
- **Spring Security:** Authentication and Authorization.
- **PostgreSQL:** Persistent user and token data.
- **Redis:** Fast storage for token revocation and login attempts.
- **Kafka:** Event-driven notifications for emails.
- **Flyway:** Database schema versioning.

## 📂 API Endpoints

### Public Endpoints
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| POST | `/auth/register` | Register a new local user |
| POST | `/auth/login` | Authenticate and get JWT |
| POST | `/auth/google` | Google Social Login (ID Token) |
| POST | `/auth/apple` | Apple Social Login (ID Token) |
| POST | `/auth/refresh` | Rotate access token |
| POST | `/auth/forgot-password` | Request password reset |
| POST | `/auth/reset-password` | Complete password reset |
| POST | `/auth/verify-email` | Verify account via token |

### Protected Endpoints
| Method | Endpoint | Required Role | Description |
| :--- | :--- | :--- | :--- |
| POST | `/auth/logout` | ANY | Revoke current token |
| GET | `/actuator/**` | ADMIN | System monitoring data |

## ⚙️ Configuration

The service requires the following environment variables for full functionality:

| Variable | Description | Default |
| :--- | :--- | :--- |
| `JWT_SECRET` | 256-bit key for signing tokens | (Required) |
| `GOOGLE_CLIENT_ID` | Google OAuth2 Client ID | (Required for Google Auth) |
| `APPLE_CLIENT_ID` | Apple Service ID | (Required for Apple Auth) |
| `DB_HOST` | PostgreSQL Host | `localhost` |
| `REDIS_HOST` | Redis Host | `localhost` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka Broker Address | `localhost:9092` |

## 🧪 Testing

A comprehensive integration test suite is available in the `tests/` directory:
- `test-auth-complete.sh`: Validates the full registration-to-logout lifecycle.
- `test-gateway-routes.sh`: Verifies security rules and API Gateway integration.

---
*ShopSphere — Building the future of grocery microservices.*
