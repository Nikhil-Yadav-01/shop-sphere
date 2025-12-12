# User Service

A fully decoupled microservice for user profile and address management in the ShopSphere ecosystem.

## Overview

The User Service is responsible for:
- Managing user profiles linked to authentication users
- Handling user address information
- Providing user data to other microservices via REST API
- Ensuring security through JWT token validation from API Gateway

## Architecture

### Technology Stack
- **Java 21** with Spring Boot 3.2.0
- **PostgreSQL 15** for persistence
- **Flyway** for database migrations
- **Spring Security** with JWT validation
- **MapStruct** for object mapping
- **Eureka** for service discovery
- **Docker** for containerization

### Module Structure
```
user-service/
├── src/main/java/com/rudraksha/shopsphere/user/
│   ├── UserApplication.java               # Spring Boot entry point
│   ├── controller/
│   │   ├── UserController.java            # User CRUD endpoints
│   │   └── AddressController.java         # Address management endpoints
│   ├── service/
│   │   ├── UserService.java              # User business logic interface
│   │   ├── AddressService.java           # Address business logic interface
│   │   └── impl/
│   │       ├── UserServiceImpl.java       # User business logic implementation
│   │       └── AddressServiceImpl.java    # Address business logic implementation
│   ├── repository/
│   │   ├── UserProfileRepository.java    # User data access
│   │   └── AddressRepository.java        # Address data access
│   ├── entity/
│   │   ├── UserProfile.java              # User profile JPA entity
│   │   └── Address.java                  # Address JPA entity
│   ├── dto/
│   │   ├── request/
│   │   │   ├── UpdateUserRequest.java    # User update DTO
│   │   │   └── CreateAddressRequest.java # Address creation DTO
│   │   └── response/
│   │       ├── UserResponse.java         # User response DTO
│   │       └── AddressResponse.java      # Address response DTO
│   ├── mapper/
│   │   └── UserMapper.java               # MapStruct entity-to-DTO mapper
│   ├── security/
│   │   ├── JwtTokenProvider.java         # JWT validation logic
│   │   ├── JwtAuthenticationFilter.java  # JWT authentication filter
│   │   └── SecurityConstants.java        # Security constants
│   └── config/
│       └── SecurityConfig.java           # Spring Security configuration
├── src/main/resources/
│   ├── application.yml                   # Application configuration
│   └── db/migration/
│       └── V1__create_user_tables.sql   # Database schema
├── pom.xml                               # Maven configuration
└── Dockerfile                            # Docker image definition
```

## API Endpoints

All endpoints require a valid JWT token in the `Authorization` header:
```
Authorization: Bearer <JWT_TOKEN>
```

### User Profile Endpoints

#### Get User Profile
```bash
GET /api/v1/users/{userId}
```
Response: `UserResponse`

#### Create User Profile
```bash
POST /api/v1/users?authUserId={authUserId}
```
Response: `UserResponse` (HTTP 201)

#### Update User Profile
```bash
PUT /api/v1/users/{userId}
Body: UpdateUserRequest
```
Response: `UserResponse`

#### Delete User Profile
```bash
DELETE /api/v1/users/{userId}
```
Response: HTTP 204

#### Check User Exists
```bash
GET /api/v1/users/exists/{authUserId}
```
Response: Boolean

### Address Endpoints

#### Get All User Addresses
```bash
GET /api/v1/users/{userId}/addresses
```
Response: List<`AddressResponse`>

#### Get Specific Address
```bash
GET /api/v1/users/{userId}/addresses/{addressId}
```
Response: `AddressResponse`

#### Create Address
```bash
POST /api/v1/users/{userId}/addresses
Body: CreateAddressRequest
```
Response: `AddressResponse` (HTTP 201)

#### Update Address
```bash
PUT /api/v1/users/{userId}/addresses/{addressId}
Body: CreateAddressRequest
```
Response: `AddressResponse`

#### Delete Address
```bash
DELETE /api/v1/users/{userId}/addresses/{addressId}
```
Response: HTTP 204

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | localhost | PostgreSQL host |
| `DB_PORT` | 5432 | PostgreSQL port |
| `DB_NAME` | shopsphere_users | Database name |
| `DB_USERNAME` | postgres | Database username |
| `DB_PASSWORD` | password | Database password |
| `EUREKA_URI` | http://localhost:8761/eureka | Eureka service discovery URL |
| `JWT_SECRET` | mySecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLong | JWT signing secret |
| `JWT_EXPIRATION_MS` | 3600000 | JWT token expiration (milliseconds) |

### Application Configuration
```yaml
server:
  port: 8082

spring:
  application:
    name: user-service
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  flyway:
    enabled: true
    locations: classpath:db/migration

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URI}
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true
```

## Security

### JWT Authentication Flow
1. User authenticates via Auth Service and receives JWT token
2. User includes JWT token in `Authorization: Bearer <TOKEN>` header
3. API Gateway validates token and forwards request to User Service
4. User Service extracts `JwtAuthenticationFilter` to validate token
5. Service extracts userId and roles from JWT claims
6. Request is processed with authenticated user context

### Protected Endpoints
- All `/api/v1/**` endpoints require valid JWT token
- Actuator endpoints (`/actuator/**`) are public for health checks

## Database Schema

### user_profiles Table
```sql
CREATE TABLE user_profiles (
    id UUID PRIMARY KEY,
    auth_user_id UUID NOT NULL UNIQUE,  -- Reference to auth service user
    phone VARCHAR(20),
    date_of_birth DATE,
    avatar_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### addresses Table
```sql
CREATE TABLE addresses (
    id UUID PRIMARY KEY,
    user_profile_id UUID NOT NULL REFERENCES user_profiles(id),
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(100) NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

## DTOs

### UserResponse
```json
{
  "id": "uuid",
  "auth_user_id": "uuid",
  "phone": "+1234567890",
  "date_of_birth": "1990-01-01",
  "avatar_url": "https://...",
  "addresses": [...],
  "created_at": "2025-12-12T18:00:00",
  "updated_at": "2025-12-12T18:00:00"
}
```

### UpdateUserRequest
```json
{
  "phone": "+1234567890",
  "date_of_birth": "1990-01-01",
  "avatar_url": "https://..."
}
```

### CreateAddressRequest
```json
{
  "address_line1": "123 Main St",
  "address_line2": "Apt 4B",
  "city": "New York",
  "state": "NY",
  "postal_code": "10001",
  "country": "USA",
  "is_default": true
}
```

### AddressResponse
```json
{
  "id": "uuid",
  "address_line1": "123 Main St",
  "address_line2": "Apt 4B",
  "city": "New York",
  "state": "NY",
  "postal_code": "10001",
  "country": "USA",
  "is_default": true,
  "created_at": "2025-12-12T18:00:00",
  "updated_at": "2025-12-12T18:00:00"
}
```

## Building and Running

### Local Development
```bash
# Build the project
cd user-service
mvn clean package

# Run the application
java -jar target/user-service-1.0.0-SNAPSHOT.jar
```

### Docker
```bash
# Build Docker image
docker build -f user-service/Dockerfile -t user-service:1.0.0 .

# Run Docker container
docker run -e DB_HOST=localhost -e EUREKA_URI=http://localhost:8761/eureka \
  -p 8082:8082 user-service:1.0.0
```

### Docker Compose
```bash
# Start all services including user-service
docker-compose up -d

# Check service health
curl http://localhost:8082/actuator/health
```

## Health Check

```bash
curl http://localhost:8082/actuator/health
```

Response:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    }
  }
}
```

## Metrics

Available at `/actuator/metrics`:
```bash
curl http://localhost:8082/actuator/metrics
```

## Dependencies

### Core
- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Security
- Spring Boot Starter Validation
- Spring Boot Starter Actuator

### Database
- PostgreSQL Driver
- Flyway Core
- Flyway PostgreSQL

### Integration
- Spring Cloud Starter Netflix Eureka Client

### Security
- JJWT (JSON Web Token) - API, Implementation, Jackson

### Utilities
- Lombok
- MapStruct

### Testing
- Spring Boot Starter Test
- Spring Security Test
- H2 Database

## Service Integration

### With API Gateway
The API Gateway routes requests to the User Service:
```
Client → API Gateway → User Service
         (validates JWT)
```

### With Auth Service
- User Service validates JWT tokens issued by Auth Service
- Uses same JWT secret for token verification
- Extracts userId from token subject claim

### With Other Services
User Service can be called by other microservices using Eureka discovery:
```java
@FeignClient("user-service")
public interface UserServiceClient {
    @GetMapping("/api/v1/users/{userId}")
    UserResponse getUser(@PathVariable UUID userId);
}
```

## Troubleshooting

### Connection to Database Failed
- Check `DB_HOST`, `DB_PORT`, `DB_USERNAME`, `DB_PASSWORD`
- Ensure PostgreSQL container is running and healthy
- Verify network connectivity

### JWT Token Validation Failed
- Check `JWT_SECRET` matches Auth Service
- Verify token not expired
- Ensure `Authorization` header format: `Bearer <TOKEN>`

### Eureka Registration Failed
- Check `EUREKA_URI` is accessible
- Verify Discovery Service is running
- Check network connectivity

### Migration Failed
- Verify database user has DDL permissions
- Check migrations in `src/main/resources/db/migration`
- Ensure database exists

## Contributing

When modifying the User Service:
1. Maintain separation of concerns (controllers, services, repositories, entities)
2. Use DTOs for request/response
3. Add proper validation in request DTOs
4. Update database migrations for schema changes
5. Keep security configuration aligned with API Gateway requirements

## Version
1.0.0-SNAPSHOT

## Created
2025-12-12
