# AI Workflow Rules — ShopSphere Grocery

These rules govern how AI agents interact with the ShopSphere codebase. Any AI working on this project MUST follow these guidelines.

---

## 1. Task Approach

### 1.1 Before Starting Any Task

1. **Read `project-overview.md`** to understand the system
2. **Read `architecture-context.md`** for service boundaries and patterns
3. **Read `code-standards.md`** for conventions to follow
4. **Read `progress-tracker.md`** to understand current state and avoid duplicate work
5. **Check if `project-report.md`** references the area you're working on (it contains audit findings)

### 1.2 Understanding Before Action

- Read the full source file before editing — never assume structure
- Read neighboring files in the same package to understand patterns
- Check `pom.xml` before adding new dependencies
- Check `application.yml` before adding new configuration

### 1.3 Making Changes

- One logical change per commit
- Update `progress-tracker.md` when starting and completing work
- Never introduce secrets or hardcoded credentials
- Never skip adding proper error handling
- Add or update tests for every change

---

## 2. Code Quality Rules

### 2.1 Prohibited

- No hardcoded secrets, passwords, API keys in source code (use env vars or Vault)
- No `System.out.println()` — use SLF4J (`log.info`, `log.debug`, `log.warn`, `log.error`)
- No `Thread.sleep()` in production code
- No fire-and-forget Kafka sends without error handling
- No `try { ... } catch (Exception e) { /* empty */ }`
- No raw `Map<String, Object>` for event payloads — use typed DTOs
- No `@Data` on JPA `@Entity` classes — use `@Getter` `@Setter` and explicit `equals`/`hashCode`
- No `@Transactional` on class-level — be explicit per method
- No `@Value` with dangerous defaults (`${prop:default}` where default is a secret)

### 2.2 Required Patterns

- Every public endpoint needs validation (`@Valid` on request DTOs)
- Every service needs a `GlobalExceptionHandler` with `@RestControllerAdvice`
- Every Kafka producer must have: `acks=all`, retries configured, error logging
- Every Kafka consumer must have: manual commit, DLQ topic, idempotency handling
- Every inter-service call needs: circuit breaker, timeout, retry policy
- All list endpoints must support pagination (`Pageable`)
- All entities should have `@Version` for optimistic locking
- All mutation operations should be idempotent (idempotency key pattern)

### 2.3 Testing Requirements

- Every service must have unit tests (JUnit 5 + Mockito)
- Integration tests for database and Kafka interaction (Testcontainers)
- Contract tests for inter-service API boundaries (Pact)
- Load tests for critical paths (k6 or Gatling)

---

## 3. Service Interaction Rules

### 3.1 Event-Driven Communication

- Services communicate asynchronously via Kafka for all business events
- Never make synchronous HTTP calls between services for business operations
- Use the outbox pattern for reliable event publication (DB + Kafka consistency)
- Every event must have a schema version field

### 3.2 Synchronous Calls (Exception Cases)

Only allowed for:
- API Gateway -> Service (external request routing)
- Authentication validation (but prefer JWT self-validation)

### 3.3 Kafka Conventions

- Topic naming: `{domain}.{action}` (e.g., `order.placed`, `payment.success`)
- Event format: Avro with Schema Registry, or validated JSON with schema version
- Producer config: `acks=all`, `retries=Integer.MAX_VALUE`, `delivery.timeout.ms=30000`
- Consumer config: `enable.auto.commit=false`, manual commit after processing
- Every consumer group needs a DLQ topic: `{group}.DLQ`

---

## 4. Security Rules

- Every service must have Spring Security configured
- Public endpoints explicitly listed, all others require authentication
- JWT tokens must use `roles` (List) claim, NOT `role` (String)
- All secrets must come from environment variables or Vault
- Never commit `.env` files
- SQL migrations must not contain sensitive data in plain text

---

## 5. Project Navigation

| Context | File |
|---|---|
| System overview | `project-overview.md` |
| Architecture decisions | `architecture-context.md` |
| Code style guide | `code-standards.md` |
| Task tracking | `progress-tracker.md` |
| UI/frontend | `ui-context.md` |
| Audit findings | `project-report.md` |
| Service specifications | `dependency.txt` |
| Audit checklist | `auditing.txt` |

---

## 6. When Stuck

- Read the `project-report.md` — it documents all known issues
- Read `dependency.txt` — the authoritative service specification
- Read `auditing.txt` — the service audit checklist
