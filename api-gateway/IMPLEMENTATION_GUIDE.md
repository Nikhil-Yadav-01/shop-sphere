# API Gateway Implementation Guide

## Overview
This API Gateway implements all critical requirements specified in the architecture:
- JWT validation with caching
- Rate limiting per user/IP
- Request/response logging with correlation IDs
- Circuit breaker pattern for resilience
- Centralized error handling
- Config-driven routing
- Role-based access control

## Architecture

### Filter Chain (Execution Order)
1. **CorrelationIdFilter** (HIGHEST_PRECEDENCE) - Injects correlation/trace IDs
2. **LoggingFilter** (HIGHEST_PRECEDENCE + 1) - Logs incoming requests
3. **JwtClaimsFilter** (HIGHEST_PRECEDENCE + 2) - Extracts JWT claims to headers
4. **HealthCheckFilter** (HIGHEST_PRECEDENCE + 3) - Validates downstream service health
5. **AuthenticationFilter** - Validates JWT tokens (with caching & retry)
6. **RateLimitingFilter** - Enforces rate limits via Redis
7. **RoleBasedAccessFilter** - Validates user roles
8. **RequestTimeoutFilter** - Enforces request timeouts
9. **MetricsFilter** (LOWEST_PRECEDENCE) - Records Prometheus metrics

### Filters Implemented

#### 1. CorrelationIdFilter
- Generates unique `X-Correlation-ID` and `X-Trace-ID` headers
- Propagates across all downstream services
- Essential for distributed tracing
- Order: HIGHEST_PRECEDENCE

#### 2. LoggingFilter
- Structured logging with JSON format
- Logs method, path, status code, latency, correlation ID
- Separate warning logs for 4xx/5xx errors
- Order: HIGHEST_PRECEDENCE + 1

#### 3. JwtClaimsFilter
- Decodes JWT without validating signature (assumes Auth-Service validated)
- Extracts userId, roles, permissions into request headers:
  - `X-User-Id`
  - `X-User-Roles` (comma-separated)
  - `X-User-Permissions` (comma-separated)
- Consumed by downstream services
- Order: HIGHEST_PRECEDENCE + 2

#### 4. AuthenticationFilter
- Validates JWT tokens with Auth-Service
- **Caching**: Stores valid tokens in Redis for 30 minutes
- **Retry Logic**: Exponential backoff (2 retries, 100ms base, 1s max)
- **Timeout**: Configurable per `auth.request.timeout` (default 5s)
- Returns 401 Unauthorized if invalid
- Order: Selective (applied per route)

#### 5. RateLimitingFilter
- Rate limiting using Redis sliding window counter
- Identifies clients by:
  - User ID (if authenticated)
  - Client IP (fallback)
- Default: 100 requests/minute (configurable)
- Returns 429 Too Many Requests when exceeded
- Headers added: `X-Rate-Limit-Remaining`, `X-Rate-Limit-Reset`
- Order: Selective (can be applied per route)

#### 6. RoleBasedAccessFilter
- Validates user roles for protected routes
- Extracts roles from `X-User-Roles` header
- Supports multiple roles (comma-separated)
- Returns 403 Forbidden if user lacks required role
- Order: Selective (applied per route)

#### 7. HealthCheckFilter
- Validates downstream service availability
- Uses Eureka service discovery
- Caches health status (poor-man's circuit breaker)
- Returns 503 Service Unavailable if service unhealthy
- Order: HIGHEST_PRECEDENCE + 3

#### 8. RequestTimeoutFilter
- Enforces request timeout (default 30s)
- Per-route timeout override supported
- Returns 504 Gateway Timeout on timeout
- Order: Selective

#### 9. MetricsFilter
- Prometheus metrics collection
- Tracks:
  - Request latency (p50, p95, p99)
  - Total request count
  - Per-method, per-path, per-status metrics
- Metrics endpoint: `/actuator/prometheus`
- Order: LOWEST_PRECEDENCE

## Configuration

### application.yml
All routes are configured in `application.yml` under `gateway.routes`:

```yaml
gateway:
  routes:
    - name: auth-service
      path: /auth/**
      uri: lb://auth-service
      authenticated: false
      stripPrefix: false
    
    - name: admin-service
      path: /admin/**
      uri: lb://admin-service
      authenticated: true
      stripPrefix: false
      roles:
        - ADMIN
```

**Route Properties:**
- `name`: Unique route identifier
- `path`: Request path pattern
- `uri`: Load-balanced service name (lb://)
- `authenticated`: Require JWT validation
- `stripPrefix`: Remove path prefix before forwarding
- `roles`: Required roles (optional)

### Environment Variables

| Variable | Default | Purpose |
|----------|---------|---------|
| `REDIS_HOST` | localhost | Redis server host |
| `REDIS_PORT` | 6379 | Redis server port |
| `REDIS_PASSWORD` | (empty) | Redis authentication |
| `AUTH_SERVICE_URL` | http://localhost:8081 | Auth-Service endpoint |
| `AUTH_REQUEST_TIMEOUT` | 5000 | JWT validation timeout (ms) |
| `JWT_SECRET` | your-secret-key-change-in-production | JWT decoding secret |
| `RATE_LIMIT_RPM` | 100 | Rate limit (requests/min) |
| `EUREKA_URI` | http://localhost:8761/eureka | Service discovery |

## Dependencies Added

```xml
<!-- Redis -->
spring-boot-starter-data-redis
jedis

<!-- JWT -->
jjwt-api, jjwt-impl, jjwt-jackson

<!-- Resilience -->
resilience4j-spring-boot3
resilience4j-circuitbreaker
resilience4j-timelimiter

<!-- Logging -->
spring-boot-starter-logging

<!-- Config -->
spring-cloud-starter-config

<!-- Metrics -->
micrometer-registry-prometheus
```

## Usage Examples

### Protecting an Endpoint (Role-Based)
```yaml
- name: admin-service
  path: /admin/**
  uri: lb://admin-service
  authenticated: true
  stripPrefix: false
  roles:
    - ADMIN
```

Request must have:
- Valid JWT token in `Authorization: Bearer <token>` header
- JWT decoded to have `ADMIN` in roles claim

### Public Endpoint with Rate Limiting
```yaml
- name: catalog-service
  path: /catalog/**
  uri: lb://catalog-service
  authenticated: false
  stripPrefix: true
```

Rate limiting automatically applied to all routes via RateLimitingFilter.

### Protected Endpoint (Any Authenticated User)
```yaml
- name: order-service
  path: /order/**
  uri: lb://order-service
  authenticated: true
  stripPrefix: true
```

Request must have valid JWT; roles optional.

## Observability

### Request Tracing
Every request gets unique `X-Correlation-ID` and `X-Trace-ID`:
```
X-Correlation-ID: 550e8400-e29b-41d4-a716-446655440000
X-Trace-ID: 6ba7b810-9dad-11d1-80b4-00c04fd430c8
```

### Logging
Logs are structured JSON format (configurable in application.yml):
```
[2025-01-15 10:23:45.123] [main] DEBUG com.rudraksha.shopsphere.gateway.filter.LoggingFilter - Incoming request - Method: GET, Path: /catalog/products, CorrelationId: 550e8400..., UserId: user123
[2025-01-15 10:23:45.456] [main] INFO com.rudraksha.shopsphere.gateway.filter.LoggingFilter - Request completed - Method: GET, Path: /catalog/products, Status: 200, Duration: 333ms, CorrelationId: 550e8400...
```

### Metrics
Access Prometheus metrics at `/actuator/prometheus`:
```
gateway_request_get_catalog_products_seconds_max{method="GET",path="/catalog/products",status="200"} 0.5
gateway_requests_total{method="GET",path="/catalog/products",status="200"} 42.0
```

## Resilience Patterns

### Circuit Breaker
Configured via resilience4j in application.yml:
- Failure threshold: 50%
- Slow call threshold: 50%
- Wait duration in open state: 30s
- Slow call duration threshold: 5s
- Minimum calls to trigger: 10

Circuit breaker states transition:
- CLOSED → OPEN (when failure rate exceeds threshold)
- OPEN → HALF_OPEN (after wait duration)
- HALF_OPEN → CLOSED (if calls succeed)
- HALF_OPEN → OPEN (if calls still fail)

### JWT Validation Retry
- Retry up to 2 times
- Exponential backoff: 100ms, 200ms
- Max backoff: 1s
- Only retries on timeout (not on 401)

### Rate Limiting Grace
- Allows burst up to configured limit
- Per-minute sliding window
- Cached in Redis with auto-expiry

## Testing

Run tests:
```bash
mvn test
```

Test coverage includes:
- Missing/invalid authentication headers
- JWT token validation
- Rate limiting enforcement
- Role-based access control

## Deployment

### Docker
```bash
docker build -t shopsphere/api-gateway:1.0.0 .
docker run -p 8080:8080 \
  -e REDIS_HOST=redis \
  -e AUTH_SERVICE_URL=http://auth-service:8081 \
  -e JWT_SECRET=<production-secret> \
  shopsphere/api-gateway:1.0.0
```

### Kubernetes
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: gateway-config
data:
  application.yml: |
    gateway:
      routes: [...]

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-gateway
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: gateway
        image: shopsphere/api-gateway:1.0.0
        env:
        - name: REDIS_HOST
          value: redis.default.svc.cluster.local
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: jwt-secret
              key: secret
```

## Security Checklist

- [x] JWT validation on protected routes
- [x] Rate limiting per user/IP
- [x] Role-based access control
- [x] Request timeout enforcement
- [x] Correlation ID for audit trails
- [x] Structured logging
- [x] Service health validation
- [x] Centralized error handling
- [x] JWT caching (reduces auth-service load)
- [x] Graceful degradation on Redis failure

## Future Enhancements

1. **OAuth 2.0/OIDC Support** - External identity provider integration
2. **API Key Management** - Non-interactive client authentication
3. **Request Signing** - HMAC request signature verification
4. **GraphQL Support** - Unified query language
5. **API Versioning** - Content negotiation for API versions
6. **Request Transformation** - Header/body manipulation per route
7. **Mock Mode** - Local testing without downstream services
8. **Analytics** - Request patterns, error trends, user behavior
