# Code Standards — ShopSphere Grocery

## Language & Runtime

- **Language**: Java 21
- **Framework**: Spring Boot 3.2.0
- **Build**: Maven (no Gradle)
- **Java Version**: 21 features allowed (records, sealed classes, pattern matching, text blocks)

---

## Project Structure

### Maven Module Layout

Each service follows this structure:

```
{service-name}/
  pom.xml
  Dockerfile
  src/main/java/com/rudraksha/shopsphere/{service}/
    {Service}Application.java
    config/
    controller/
    dto/
      request/
      response/
    entity/
    exception/
    kafka/
    repository/
    service/
      impl/
  src/main/resources/
    application.yml
    application-{profile}.yml
    db/migration/          # (PostgreSQL services only)
      V1__{name}.sql
  src/test/java/
    ...                    # (to be added)
```

### Package Naming

- Base: `com.rudraksha.shopsphere.{serviceName}`
- Shared utilities: `com.rudraksha.shopsphere.shared`
  - Location: **Must be a proper shared library/module**, not copy-pasted
  - Currently duplicated in auth-service and admin-service — consolidate into a shared library

---

## Coding Conventions

### Naming

| Element | Convention | Example |
|---|---|---|
| Classes | PascalCase | `InventoryServiceImpl` |
| Methods | camelCase | `reserveInventoryForOrder` |
| Constants | UPPER_SNAKE_CASE | `TOKEN_PREFIX` |
| Enums | PascalCase (class), UPPER_SNAKE_CASE (values) | `OrderStatus.PENDING` |
| Packages | lowercase | `com.rudraksha.shopsphere.inventory` |
| Interface/Impl | `{Entity}Service` / `{Entity}ServiceImpl` | `InventoryService` / `InventoryServiceImpl` |
| DTOs | `{Action}{Entity}{Type}` | `CreateInventoryRequest`, `InventoryResponse` |
| URIs | kebab-case | `/api/v1/products`, `/auth/login` |

### Method Conventions

- `create{Entity}` — returns created entity response
- `get{Entity}By{Field}` — returns single entity
- `getAll{Entities}` — returns paginated list
- `update{Entity}` — returns updated entity
- `delete{Entity}` — returns void
- Booleans: `existsBy{Field}`, `is{State}`, `has{Property}`

### Validation

- Always use request DTOs with `jakarta.validation` annotations (`@NotBlank`, `@NotNull`, `@Size`, `@Email`, `@Positive`, `@DecimalMin`)
- Use `@Valid` on controller method parameters for nested validation
- Never validate in controllers — delegate to service layer
- Use `@Validated` for group validation if needed

### Exception Handling

- Custom exceptions per service: `{Domain}Exception` extends `RuntimeException`
- One `GlobalExceptionHandler` per service with `@RestControllerAdvice`
- HTTP status codes:
  - 400: Validation errors, bad requests
  - 401: Authentication failures (use `AuthException` in auth-service)
  - 404: Entity not found
  - 409: Conflict (duplicate resource)
  - 500: Unexpected errors (log and return generic message)

---

## Database Standards

### PostgreSQL
- Use Flyway for all schema migrations
- `ddl-auto: validate` in production
- Migrations in `src/main/resources/db/migration/V{version}__{description}.sql`
- Always use `IF NOT EXISTS` for idempotent creation
- Include `CHECK` constraints for enum fields
- Include indexes on FK columns and query columns
- Use `SERIAL` or `BIGSERIAL` for PK, or `UUID` type

### MongoDB
- Use Spring Data MongoDB repositories
- `@Document` / `@CompoundIndex` annotations
- Enable `auto-index-creation` or manage via migration scripts
- Add TTL indexes for time-series data

### Redis
- Use `@RedisHash` for domain entities
- Always configure TTL (`@TimeToLive`)
- Use hash data structures for complex objects
- Never use JDK serialization — configure JSON serialization

---

## Concurrency Standards

### Required on Every Entity with Concurrent Writes

```java
@Version
private Long version;       // Optimistic locking
```

### Required on Every Inventory/Cart/Coupon Mutation

```java
// Use Redis distributed lock for critical sections
distributedLockUtil.executeWithLock(sku, () -> {
    // read -> modify -> write
});
```

### Required on Every Kafka Consumer

```java
// Idempotency check
if (processedEventRepository.existsByIdempotencyKey(event.getId())) {
    log.debug("Duplicate event skipped: {}", event.getId());
    ack.acknowledge();
    return;
}
```

---

## Kafka Standards

### Producer Configuration
```yaml
spring.kafka.producer:
  acks: all
  retries: 2147483647
  delivery.timeout.ms: 30000
  max.block.ms: 5000
  properties:
    enable.idempotence: true
```

### Consumer Configuration
```yaml
spring.kafka.consumer:
  enable.auto.commit: false
  properties:
    isolation.level: read_committed
```

### Event Structure
```java
public class EventEnvelope<T> {
    private String id;          // UUID, idempotency key
    private String type;        // "order.placed"
    private T payload;          // Typed payload
    private String schemaVersion; // "1.0"
    private Instant timestamp;
    private String source;      // Service name
}
```

---

## API Standards

### URL Convention
```
/api/v1/{resource}[/{id}][/{subresource}]
/api/v1/products/{id}
/api/v1/products/{id}/variants
```

### Pagination
```java
// Repository
Page<Entity> findAll(Pageable pageable);
Page<Entity> findByCustomerId(Long customerId, Pageable pageable);

// Response
public record PagedResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    long totalPages,
    boolean hasNext,
    boolean hasPrevious
) {}
```

### Idempotency
```java
// POST requests should accept an idempotency key
@PostMapping
public ResponseEntity<OrderResponse> createOrder(
    @Valid @RequestBody CreateOrderRequest request,
    @RequestHeader("Idempotency-Key") String idempotencyKey
) { ... }
```

---

## Logging Standards

```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderServiceImpl {
    // Use structured logging
    log.info("Order created: orderId={}, customerId={}", order.getId(), request.getCustomerId());
    log.warn("Payment failed: transactionId={}, reason={}", txId, reason);
    log.error("Kafka publish failed", exception);  // Include exception as last arg
}
```

---

## Testing Standards

### Unit Tests
- JUnit 5 + Mockito
- Mock all dependencies
- Test service logic in isolation
- Cover boundary conditions (null, empty, edge values)

### Integration Tests
- Testcontainers for PostgreSQL, MongoDB, Redis, Kafka
- Spring Boot `@SpringBootTest`
- Test full request-response cycle

### Contract Tests
- Spring Cloud Contract or Pact for inter-service APIs

### Coverage Targets
- Service layer: 100% of business logic paths
- Controller layer: 100% of endpoint response codes
- Exception handler: All custom exception mappings
- Global minimum: 80% line coverage

---

## Docker Standards

```dockerfile
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -g 1001 appuser && \
    adduser -u 1001 -G appuser -s /sbin/nologin -D appuser && \
    apk add --no-cache curl
WORKDIR /app
USER appuser
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
EXPOSE {PORT}
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:{PORT}/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

---

## Git Conventions

- Branch naming: `{type}/{description}` where type is `fix/`, `feat/`, `chore/`
- Commit messages: `{type}({scope}): {description}` e.g., `fix(auth): wire JWT filter into security chain`
- One logical change per commit
- Never commit directly to main/master
