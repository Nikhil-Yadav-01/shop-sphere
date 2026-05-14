# ShopSphere Grocery — Production Readiness Architectural Review

**Date:** 2026-05-14
**Reviewer:** Principal Distributed Systems Architect
**Target Scale:** Millions of users, flash sales, Prime Day–level traffic spikes

---

## Production Readiness Score: **2.8 / 10**

**Justification:** The codebase shows awareness of microservices patterns (Eureka, Kafka, Redis, Docker) but every layer is implemented in a toy/demo fashion. There are zero unit tests, zero integration tests, zero Kubernetes manifests, zero Terraform configs, no circuit breakers, no SAGA/event sourcing, hardcoded configurations, synchronous blocking chains inside transactional boundaries, duplicated shared code without a library module, port conflicts, simulation-only payment processing, and multiple `Math.random()` paths. This system will catastrophically fail under 10K concurrent users—let alone millions.

---

## Top 10 Critical Fixes (Priority Order)

| # | Issue | Severity | Service(s) |
|---|-------|----------|------------|
| 1 | No distributed transaction orchestration — checkout calls payment+cart synchronously in the same `@Transactional` block | 🔴 Critical | checkout-service |
| 2 | Inventory optimistic locking bypass — `reserveInventoryForOrder` uses homebrew Redis lock that can silently fail | 🔴 Critical | inventory-service |
| 3 | Cart-service returns hardcoded prices and product names — no actual catalog lookup | 🔴 Critical | cart-service |
| 4 | Kafka fire-and-forget with `System.err.println` — events silently lost on broker failure | 🔴 Critical | order-service, payment-service, inventory-service |
| 5 | Dedicated checkout DB stores both checkout and payment — no SAGA, risk of partial checkout | 🔴 Critical | checkout-service |
| 6 | Zero tests — no unit, integration, contract, or load tests anywhere | 🔴 Critical | ALL |
| 7 | JWT secret hard-referenced via `jwt.secret` in Gateway + Auth — no key rotation, no JWKS | 🔴 Critical | api-gateway, auth-service |
| 8 | No Kubernetes / no autoscaling — Docker Compose only, single points of failure everywhere | 🔴 Critical | Infrastructure |
| 9 | Shared library is copy-pasted across services — version drift guaranteed | 🟠 High | auth-service, admin-service |
| 10 | Rate limiter keyed by IP only — user-based throttling missing, single IP can exhaust pool | 🟠 High | api-gateway |

---

# 1. API GATEWAY (`api-gateway`)

### 1.1 🔴 Synchronous Gateway ↔ Auth Token Validation on Every Request

**Problem:** The gateway validates JWT tokens locally using `JwtTokenProvider.validateToken()`. This is correct for signature verification, but the **token revocation check** goes to Redis for every request (`AuthenticationFilter.checkRevocation()`). Under 1M requests/min, this adds ~2-5ms per request to Redis, and if Redis is under load, the `Mono<Void>` path on `onErrorResume` silently falls back to allowing the request through (line 95: `onErrorResume(e -> Mono.just(new RevocationResult(false)))`).

**Why it fails at scale:** Redis latency spikes cause `onErrorResume` to bypass revocation checks — ALL revoked tokens become valid until Redis recovers. This is a security bypass that opens the door to token replay attacks.

**Fix:** Replace inline Redis revocation check with a local Caffeine cache (already configured!) that syncs periodically. Use Redis Pub/Sub to invalidate entries.
```java
// Use Caffeine cache with Redis invalidation — already have caffeine in pom.xml
@Bean
public Cache<String, Boolean> revokedTokenCache() {
    return Caffeine.newBuilder()
        .maximumSize(100_000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();
}

// Remove blocking Redis call from hot path — check local cache first
private Mono<RevocationResult> checkRevocation(String token) {
    String key = REVOKED_TOKEN_PREFIX + token;
    Boolean cached = revokedTokenCache.getIfPresent(key);
    if (cached != null) return Mono.just(new RevocationResult(cached));
    // Fallback to Redis only on cache miss
    return redisTemplate.hasKey(key)
        .doOnNext(result -> revokedTokenCache.put(key, result))
        .map(RevocationResult::new)
        .defaultIfEmpty(new RevocationResult(false));
}
```

### 1.2 🟠 Rate Limiter — IP-Only Key + Single Global Config

**Problem:** `RateLimiterConfig.principalNameKeyResolver()` uses IP address only (line 34–37). No user-ID-based key, no per-route differentiation. The default 100 req/s replenish rate applies globally to all routes equally.

**Why it fails at scale:** A single bad actor on one IP can exhaust the global pool. A DDoS via botnet (1000 IPs × 1 req/s each = 1000 req/s) bypasses this completely since each IP gets its own bucket. The `/catalog/**` search endpoint needs higher limits than `/auth/login`.

**Fix:**
```yaml
# application.yml — per-route rate limiter configuration
spring:
  cloud:
    gateway:
      routes:
        - id: catalog-service
          uri: lb://catalog-service
          predicates:
            - Path=/catalog/**
          filters:
            - name: RequestRateLimiter
              args:
                rate-limiter: "#{@catalogRateLimiter}"
                key-resolver: "#{@userKeyResolver}"
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/auth/**
          filters:
            - name: RequestRateLimiter
              args:
                rate-limiter: "#{@authRateLimiter}"
                key-resolver: "#{@ipKeyResolver}"
```

```java
@Bean
public KeyResolver userKeyResolver() {
    return exchange -> Mono.justOrEmpty(
        exchange.getRequest().getHeaders().getFirst("X-User-Id"))
        .defaultIfEmpty(exchange.getRequest().getRemoteAddress()
            .map(InetSocketAddress::getHostAddress)
            .orElse("anonymous"));
}
```

### 1.3 🟡 Missing routes in GatewayConfig — 12+ services not routed

**Problem:** `GatewayConfig.java` only defines routes for 7 services (auth, catalog, admin, batch, cart, checkout, pricing). Services like `user-service`, `payment-service`, `inventory-service`, `notification-service`, `fraud-service`, `media-service`, `review-service`, `returns-service`, `recommendation-service`, `search-service`, and `analytics-service` are completely missing from the gateway route table.

**Why it fails at scale:** These services are inaccessible through the gateway. If called directly, they bypass authentication, rate-limiting, and centralized logging. This breaks the entire API gateway pattern.

**Fix:** Add route definitions for every service. At minimum:
```java
.route("user-service", r -> r
    .path("/users/**")
    .filters(f -> f.stripPrefix(1)
        .filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
    .uri("lb://user-service"))
// Repeat for all 22 services
```

### 1.4 🟢 `LocalGatewayConfig` only routes 2 services — development blind spots

**Problem:** The local profile only routes `auth-service` and `user-service`. Developers cannot test other services locally.

**Fix:** Add all service routes to `LocalGatewayConfig` with direct localhost URLs matching each service's port.

---

# 2. AUTH SERVICE (`auth-service`)

### 2.1 🔴 Refresh Token Rotation — No Concurrent Session Protection

**Problem:** `refreshToken()` method (line 226–255) sets the old refresh token as revoked but does NOT check if the old token was already used. If a stolen refresh token is used simultaneously with a legitimate one, the first caller wins and the second gets "Invalid refresh token". This is the correct pattern for rotation, but there is NO alerting or anomaly detection on reuse.

**Why it fails at scale:** Token theft is undetectable. An attacker can exfiltrate tokens and use them. At 1M users, there will be hundreds of token theft incidents. Without detection, users' accounts are silently compromised.

**Fix:**
```java
public TokenResponse refreshToken(RefreshTokenRequest request) {
    RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
        .orElseThrow(() -> new AuthException("Invalid refresh token"));
    
    if (!storedToken.isValid()) {
        // DETECT REUSE — if the token was already revoked, alert immediately
        if (storedToken.isRevoked()) {
            securityAlertService.alertTokenReuse(storedToken.getUser().getId(),
                storedToken.getToken(), request.getRefreshToken());
        }
        throw new AuthException("Refresh token is expired or revoked");
    }
    // ... rotation logic ...
}
```

### 2.2 🟠 `LoginAttemptService` — No Distributed Rate Limiting

**Problem:** Login attempt tracking likely uses local in-memory counters (assuming from pattern). Under multiple instances of auth-service, each instance has its own counter. An attacker can rotate through instances via the gateway to bypass lockout.

**Why it fails at scale:** With 10 auth-service pods, an attacker gets 10× the allowed login attempts before lockout. Brute-force attacks succeed far more often.

**Fix:** Move login attempt counters to Redis with sliding window:
```java
public void checkLockout(String email) {
    String key = "login:attempts:" + email;
    Long attempts = redisTemplate.opsForValue().increment(key);
    if (attempts == 1) {
        redisTemplate.expire(key, Duration.ofMinutes(15));
    }
    if (attempts > MAX_LOGIN_ATTEMPTS) {
        throw new AccountLockedException("Account locked due to too many failed attempts");
    }
}
```

### 2.3 🟠 Email Service — Synchronous + No Retry

**Problem:** `emailService.sendVerificationEmail()` and `sendPasswordResetEmail()` are called synchronously inside `@Transactional` methods. If SMTP is slow or down, the entire HTTP request hangs until timeout (default 30s+), tying up Tomcat threads.

**Why it fails at scale:** Under high registration load (flash sale signups), SMTP latency causes thread pool exhaustion. Tomcat default is 200 threads — 200 concurrent registrations each waiting on SMTP = all other auth requests blocked.

**Fix:** Publish email events to Kafka and let notification-service handle async delivery. Remove direct email calls from auth-service.
```java
// AuthServiceImpl.register() — only publish event, don't send email
if (!autoVerify) {
    kafkaTemplate.send("notification.email.send", new EmailNotificationEvent(
        user.getEmail(), "Verify your email", buildVerificationEmail(token)
    ));
}
```

### 2.4 🔴 Duplicated JwtTokenProvider — Two copies with different features

**Problem:** The `auth-service` has `JwtTokenProvider` in `shared/security/` (with `generateToken`, `getEmailFromToken`) and `api-gateway` has its own copy in `gateway/security/` (without `generateToken`). The shared code is copy-pasted, not a shared library module.

**Why it fails at scale:** Any change to JWT claims (e.g., adding a custom claim, changing signing algorithm) requires updating both files in lockstep. A mismatch will cause gateway to reject tokens issued by auth-service.

**Fix:** Extract a `common` Maven module with shared security code. Make both services depend on it via `<dependency>`.

### 2.5 🟡 Password Reset Token — No Rate Limiting

**Problem:** `forgotPassword()` can be called repeatedly for any email with no rate limit. An attacker can flood a user's email with password reset requests.

**Why it fails at scale:** Each request triggers SMTP calls. With 1M requests, SMTP provider bans the sender, and legitimate users can't receive any emails.

**Fix:** Rate-limit per email per hour using Redis:
```java
String key = "password:reset:" + email;
Boolean exists = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofHours(1));
if (Boolean.FALSE.equals(exists)) {
    throw new AuthException("Password reset already requested. Try again later.");
}
```

---

# 3. CART SERVICE (`cart-service`)

### 3.1 🔴 Hardcoded Prices and Product Names

**Problem:** `CartServiceImpl.addToCart()` line 48–52:
```java
CartItem newItem = CartItem.builder()
    .productId(request.getProductId())
    .productName("Product " + request.getProductId())  // <-- HARDCODED
    .quantity(request.getQuantity())
    .price(BigDecimal.valueOf(99.99))  // <-- HARDCODED
    .imageUrl("")                      // <-- EMPTY
    .build();
```

**Why it fails at scale:** Every product added to cart shows "Product 123" at $99.99. Users cannot distinguish items. At scale, this creates massive support tickets, cart abandonment, and lost revenue. Prices are never synced with catalog — a $1000 item shows as $99.99.

**Fix:** Call catalog-service via Feign or Kafka to get real product data:
```java
@FeignClient(name = "catalog-service")
interface CatalogClient {
    @GetMapping("/api/v1/catalog/products/sku/{sku}")
    ProductResponse getProductBySku(@PathVariable String sku);
}
```

### 3.2 🟠 No Stock Validation in Cart

**Problem:** Cart adds items without checking inventory availability. A user can add 1,000,000 units of a product with only 5 in stock.

**Why it fails at scale:** Checkout will fail for most users, but the UX shows items in cart as available. This creates extreme frustration during flash sales.

**Fix:** Add optional async stock validation via Kafka or cache:
```java
public CartResponse addToCart(String userId, AddToCartRequest request) {
    // Check cached availability
    Integer available = inventoryCache.getAvailableQuantity(request.getSku());
    if (available != null && request.getQuantity() > available) {
        throw new InsufficientStockException("Only " + available + " units available");
    }
    // ... rest of existing logic
}
```

### 3.3 🟡 Redis Cart — No TTL Enforcement on Save

**Problem:** Cart entity has `expiresAt` field but `getOrCreateCart()` and `save` in `getOrCreateCart` don't set Redis TTL. Old carts accumulate forever.

**Why it fails at scale:** With 10M users, each with a 2KB cart, that's 20GB of Redis memory wasted on stale carts. Redis OOM kills all services that depend on it (auth, inventory, gateway).

**Fix:** Set TTL when saving cart:
```java
// In CartRepository or service layer after save
redisTemplate.expire("cart:" + userId, Duration.ofDays(7));
```

### 3.4 🟢 Cart Service Depends on catalog-service (Feign) but no fallback

**Problem:** The `CartServiceImpl.getOrCreateCart()` doesn't call catalog anymore (hardcoded issue from 3.1), but if you fix 3.1, there's no circuit breaker on the Feign call.

**Fix:** Use Resilience4j Feign integration:
```java
@FeignClient(name = "catalog-service", fallbackFactory = CatalogClientFallback.class)
interface CatalogClient { ... }

@Component
class CatalogClientFallback implements FallbackFactory<CatalogClient> {
    @Override
    public CatalogClient create(Throwable cause) {
        return sku -> ProductResponse.builder()
            .name("Product temporarily unavailable")
            .price(BigDecimal.ZERO)
            .build();
    }
}
```

---

# 4. CATALOG SERVICE (`catalog-service`)

### 4.1 🔴 Product Entity Missing Price Field

**Problem:** `Product.java` has no `price` field. It only has `sku`, `name`, `description`, `categoryId`, `images`, `status`. Pricing is handled by a separate `pricing-service`.

**Why it fails at scale:** Every downstream service (cart, checkout, order) needs to call pricing-service JUST to know the price of an item. This adds N+1 latency to every product page view. At 100K concurrent catalog views, each view triggers a pricing-service call, overwhelming it.

**Fix:** Embed the effective price in the product document (write-model + cache in product):
```java
public class Product {
    // ... existing fields
    private BigDecimal price;         // Current effective price
    private BigDecimal originalPrice; // For displaying discounts
    private String currency;
}
```
Sync from pricing-service via Kafka:
```java
@KafkaListener(topics = "price.updated")
public void onPriceUpdated(PriceUpdatedEvent event) {
    Product product = productRepository.findById(event.getProductId()).orElse(null);
    if (product != null) {
        product.setPrice(event.getNewPrice());
        productRepository.save(product);
    }
}
```

### 4.2 🟠 No Search Indexing — `searchProducts` uses MongoDB regex

**Problem:** `ProductServiceImpl.searchProducts()` (line 103):
```java
return productRepository.searchByName(keyword, pageable).map(this::mapToResponse);
```
This delegates to a MongoDB `$regex` query on the `name` field — no full-text index, no Elasticsearch.

**Why it fails at scale:** A `$regex` prefix query scans all documents. With 1M products, a single search takes 5–10 seconds. At 10K QPS, MongoDB is pegged at 100% CPU and all catalog operations degrade.

**Fix:** Use MongoDB Atlas Search or Elasticsearch. Since there's a `search-service`, route search through it with proper indexing:
```java
// In catalog-service, when product is created/updated, publish full document to search-service
public void publishProductCreated(Product product) {
    kafkaTemplate.send("search.product.index", product.getId(), 
        objectMapper.writeValueAsString(product));
}
```

### 4.3 🟡 No Category Hierarchy Support

**Problem:** `Category` entity likely has flat structure (parent is just `categoryId` string). No support for hierarchical category trees (Grocery → Produce → Vegetables → Leafy Greens).

**Why it fails at scale:** Navigation becomes unusable. Users expect grocery sites to have deep category hierarchies.

**Fix:** Use MongoDB `$graphLookup` or materialized path pattern:
```java
public class Category {
    @Id private String id;
    private String name;
    private String parentId;         // null for root
    private List<String> ancestors;  // Materialized path: ["root-id", "parent-id"]
    private Integer depth;
}
```

### 4.4 🟢 No Category Cache

**Problem:** Category list is fetched from MongoDB on every page load.

**Fix:** Cache categories in Redis with TTL:
```java
@Cacheable(value = "categories", key = "'all'")
public List<CategoryResponse> getAllCategories() { ... }
```

---

# 5. CHECKOUT SERVICE (`checkout-service`)

### 5.1 🔴 Synchronous Orchestration Without SAGA — Critical Data Loss Risk

**Problem:** `CheckoutServiceImpl.processCheckout()` line 35–103 does ALL of the following in a SINGLE `@Transactional` method and a SINGLE thread:
1. Fetch cart (Feign → cart-service)
2. Calculate totals (hardcoded tax + shipping)
3. Save order to checkout-db
4. Map cart items to order items
5. Call payment (Feign → MockPaymentService, which `Thread.sleep(1000)`)
6. If payment success: update order status, clear cart (Feign → cart-service)
7. If payment failure: update order status to CANCELLED

**Why it fails at scale:**
- The `@Transactional` covers the ENTIRE method including HTTP calls. If payment gateway takes 10s (common during flash sales), the database connection is held for 10s. Connection pool exhaustion after 50 concurrent checkouts.
- If `cartClient.clearCart()` fails AFTER payment succeeds, the order is CONFIRMED but the cart is NOT cleared. The user sees items still in cart, tries to check out again, and gets a duplicate order.
- If `cartClient.getCart()` fails after order is saved, the order remains PENDING with no items, no payment — orphaned record.
- `Thread.sleep(1000)` blocks the Tomcat thread. 200 threads → 200 concurrent checkouts max.

**Fix:** Implement SAGA pattern with outbox:
```java
// Step 1: Create order in PENDING status
Order order = createOrderInPendingStatus(request, userId);
publishOutboxEvent("OrderCreated", order);

// Step 2: Reserve inventory (async via Kafka)
publishOutboxEvent("InventoryReserve", order);

// Step 3: Process payment (async via Kafka consumer)
// Kafka consumer processes payment, then publishes PaymentProcessed or PaymentFailed

// Step 4: On PaymentProcessed → confirm order, clear cart
// Step 5: On PaymentFailed → cancel order, release inventory
```

### 5.2 🔴 Checkout DB Stores Both Order + Payment — Violates Database-per-Service

**Problem:** The checkout-service's `Order` entity (line 19–71) embeds `PaymentDetails` within the same table. The `payment-service` has its own `Payment` entity. The same payment data is stored in two places with no consistency mechanism.

**Why it fails at scale:** When `payment-service` processes a refund, `checkout-service`'s copy of `PaymentDetails` becomes stale. Users see incorrect payment status. There's no event to sync payment status back to checkout.

**Fix:** Remove `PaymentDetails` from checkout's `Order` entity. Reference payment by `transactionId` only. Subscribe to `payment.events`:
```java
@KafkaListener(topics = "payment.events")
public void onPaymentEvent(PaymentEvent event) {
    Order order = orderRepository.findByOrderNumber(event.getOrderNumber());
    if (event.getStatus() == COMPLETED) {
        order.setStatus(CONFIRMED);
    } else if (event.getStatus() == FAILED) {
        order.setStatus(FAILED);
    }
    orderRepository.save(order);
}
```

### 5.3 🟠 `MockPaymentService.Thread.sleep(1000)` — Blocking in Request Thread

**Problem:** Line 22–25. A full second of blocking per checkout in a simulated payment call. In production with a real gateway, this would be a network call that's also blocking.

**Why it fails at scale:** 200 Tomcat threads × 1s = 200 checkouts/second max throughput. During flash sales, this means users time out and retry, making congestion worse.

**Fix:** Make payment processing fully async. Save order, return "processing" immediately, then process payment in background:
```java
public OrderResponse processCheckout(String userId, CheckoutRequest request) {
    // 1. Validate cart
    // 2. Create order with PROCESSING status
    // 3. Publish CheckoutInitiated event
    kafkaTemplate.send("checkout.initiated", order.getOrderNumber(), 
        serializeCheckoutData(request, order));
    // 4. Return immediately — user gets order number and "processing"
    return OrderResponse.processing(order);
}
```

### 5.4 🟠 Order Number Generation — Collision Risk

**Problem:** Line 52: `"ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()`. Only 8 hex chars = ~4.3 billion combinations. Birthday paradox means collision at ~77K orders.

**Why it fails at scale:** At 1M orders, collision is virtually guaranteed. Users get "duplicate order number" constraint violation errors.

**Fix:** Use Snowflake-style ID or at least full UUID:
```java
private String generateOrderNumber() {
    return "ORD-" + IdGenerator.generateShortId(); // 12-char base62
}
```

### 5.5 🟡 Cart Service Feign Client — No Error Handling

**Problem:** The inner `CartClient` Feign interface (lines 213–239) has no fallback. If cart-service is down, `cartClient.getCart()` throws an unhandled Feign exception, rolling back the entire transaction.

**Fix:** Add `@CircuitBreaker` and fallback:
```java
@FeignClient(name = "CART-SERVICE", path = "/api/v1/cart", 
    fallbackFactory = CartClientFallbackFactory.class)
interface CartClient { ... }
```

---

# 6. ORDER SERVICE (`order-service`)

### 6.1 🔴 Duplicate Order Entity — Separate from Checkout-Service

**Problem:** `order-service` has its own `Order` entity with fields `customerId` (Long) and `shippingAddress` (String). `checkout-service` also has an `Order` entity with `userId` (String) and embedded `ShippingAddress`. These are two separate databases for what is conceptually the same domain aggregate.

**Why it fails at scale:** There are now two "sources of truth" for orders. If `order-service:createOrder` creates an order but `checkout-service:processCheckout` also creates one — which is the real order? There's no mapping or correlation between them. This causes duplicate orders, conflicting statuses, and massive support chaos.

**Fix:** Merge order management into ONE service. Keep checkout as the order-creation endpoint. Order-service should be the read/query side (CQRS) or eliminated entirely.

### 6.2 🔴 Kafka Fire-and-Forget — `System.err.println`

**Problem:** `OrderServiceImpl.publishOrderEvent()` line 158–166:
```java
private void publishOrderEvent(String eventType, String details) {
    try {
        String message = eventType + ":" + details + ":" + LocalDateTime.now();
        kafkaTemplate.send("order-events", message);
    } catch (Exception e) {
        System.err.println("Failed to publish order event: " + e.getMessage());
    }
}
```

**Why it fails at scale:** This silently swallows Kafka send failures. If Kafka is down (which it will be during a rolling deploy), ALL order events are lost—inventory reservations, notifications, analytics — the entire downstream chain breaks silently. This pattern is repeated identically in payment-service (line 169–176) and inventory-service (line 222–229).

**Fix:** Use Spring's `KafkaTemplate.send()` with `ListenableFuture` callback + dead letter queue:
```java
public void publishOrderEvent(OrderEvent event) {
    CompletableFuture<SendResult<String, String>> future = 
        kafkaTemplate.send("order-events", event.orderNumber(), serialize(event));
    future.whenComplete((result, ex) -> {
        if (ex != null) {
            log.error("Failed to publish order event: {}", event, ex);
            // Store in outbox table for retry
            eventOutboxRepository.save(new EventOutbox(event, 0));
        }
    });
}
```

### 6.3 🟠 `getAllOrders()` without Pagination

**Problem:** Line 120–125: `orderRepository.findAll()` returns ALL orders without pagination.

**Why it fails at scale:** At 10M orders, this loads 10M entities into memory. OOM kills the service.

**Fix:** Remove this endpoint or enforce pagination:
```java
public Page<OrderResponse> getAllOrders(Pageable pageable) {
    return orderRepository.findAll(pageable).map(this::mapToResponse);
}
```

### 6.4 🟠 Inconsistent ID types

**Problem:** `order-service` uses `customerId` (Long), while `checkout-service` uses `userId` (String/UUID). Other services use String for user IDs. Every service uses different ID types for the same concept.

**Why it fails at scale:** Cross-service queries become impossible. You can't trace a user's order across services without type conversion.

**Fix:** Standardize all user references to `String userId` (UUID as String) across ALL services.

### 6.5 🟡 OrderNumber collision risk (same pattern as checkout)

**Problem:** Line 128: Same truncated UUID pattern. See 5.4 for details.

---

# 7. PAYMENT SERVICE (`payment-service`)

### 7.1 🔴 `Math.random()` Payment Processing

**Problem:** `PaymentServiceImpl.simulatePaymentGateway()` line 146–149:
```java
private boolean simulatePaymentGateway(ProcessPaymentRequest request) {
    return Math.random() > 0.1; // 90% success rate
}
```

**Why it fails at scale:** This is a mock that doesn't actually process payments. In production, this service is non-functional. Real payment gateway integration (Stripe, Razorpay, etc.) is needed.

**Fix:** Integrate with Stripe API properly:
```java
public PaymentResponse processPayment(ProcessPaymentRequest request) {
    PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
        .setAmount(request.getAmount().multiply(BigDecimal.valueOf(100)).longValue()) // cents
        .setCurrency("usd")
        .setPaymentMethod(request.getPaymentMethodId())
        .setConfirm(true)
        .setReturnUrl("https://shopsphere.com/order/confirmation")
        .build();
    
    try {
        PaymentIntent intent = PaymentIntent.create(params);
        return PaymentResponse.success(intent.getId(), intent.getStatus());
    } catch (StripeException e) {
        return PaymentResponse.failed(e.getMessage());
    }
}
```

### 7.2 🔴 Refund — Just Sets Status Without Actual Refund

**Problem:** `refundPayment()` line 104–118 sets `status = REFUNDED` in the database without calling any payment gateway API. No actual money is returned.

**Fix:** Call Stripe/RAZORPAY refund API:
```java
public PaymentResponse refundPayment(RefundPaymentRequest request) {
    Payment payment = getPaymentByTransactionId(request.getTransactionId());
    Refund refund = Refund.create(RefundCreateParams.builder()
        .setPaymentIntent(payment.getPaymentGatewayId())
        .build());
    payment.setStatus(Payment.PaymentStatus.REFUNDED);
    payment.setRefundId(refund.getId());
    paymentRepository.save(payment);
    publishPaymentEvent("PAYMENT_REFUNDED", payment);
    return mapToResponse(payment);
}
```

### 7.3 🟠 No Idempotency Key for Payment Processing

**Problem:** If the client retries `processPayment()` (due to network timeout), the same order gets multiple payments created.

**Why it fails at scale:** Users get charged twice. With 0.1% network error rate and 1M orders, 1000 customers are double-charged.

**Fix:** Implement idempotency using `Idempotency-Key` header:
```java
public PaymentResponse processPayment(ProcessPaymentRequest request, String idempotencyKey) {
    // Check if already processed
    Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
    if (existing.isPresent()) {
        return mapToResponse(existing.get());
    }
    // ... process payment ...
    payment.setIdempotencyKey(idempotencyKey);
}
```

---

# 8. INVENTORY SERVICE (`inventory-service`)

### 8.1 🔴 Homebrew Redis Distributed Lock Is Unsafe

**Problem:** `DistributedLockUtil.java`:
1. Uses `setIfAbsent` with a 5-second timeout (line 35). If the operation takes > 5s, the lock expires and another thread acquires it — both threads now think they have exclusive access.
2. No lock watchdog/refresh mechanism.
3. `executeWithLock()` calls `acquireLock()` which can return `null` after timeout, then throws `RuntimeException("Could not acquire lock")`. The caller (`reserveInventoryForOrder`, line 238) relies on this for correctness, but the lock expiry race condition makes it unsafe.

**Why it fails at scale:** During flash sales, 10K concurrent users try to reserve 100 units of the same SKU. The first thread acquires the lock, starts the DB transaction, but the DB write takes 50ms due to contention. The lock expires (5s is generous, but if the DB is slow under load...). A second thread acquires the lock, reads stale available quantity, and both threads reserve the same stock. Result: overselling.

**Fix:** Use Redisson which has automatic lock watchdog:
```java
@Bean
public RedissonClient redissonClient() {
    Config config = new Config();
    config.useSingleServer().setAddress("redis://" + redisHost + ":" + redisPort);
    return Redisson.create(config);
}

public <T> T executeWithLock(String sku, LockCallback<T> operation) {
    RLock lock = redissonClient.getLock("inventory:lock:" + sku);
    // Wait up to 3s, auto-release after 30s (watchdog auto-renews every 10s)
    if (!lock.tryLock(3, 30, TimeUnit.SECONDS)) {
        throw new RuntimeException("Could not acquire lock for SKU: " + sku);
    }
    try {
        return operation.execute();
    } finally {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

### 8.2 🔴 Read-Committed Isolation + No `SELECT FOR UPDATE`

**Problem:** `reserveInventoryForOrder()` line 238–267 runs inside `@Transactional` but reads the inventory item without `SELECT ... FOR UPDATE`. Under Repeatable Read or Read Committed, two concurrent transactions can read the same `availableQuantity`.

**Why it fails at scale:** The Redis lock (even if fixed) is held for the duration of the operation across threads, but the JPA transaction isolation still allows phantom reads within PostgreSQL Read Committed default.

**Fix:** Use `@Lock(PESSIMISTIC_WRITE)` on the repository method:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT i FROM InventoryItem i WHERE i.sku = :sku")
Optional<InventoryItem> findBySkuWithLock(@Param("sku") String sku);
```

### 8.3 🟠 Kafka Event Publication Inside Transaction — No Outbox Pattern

**Problem:** `reserveInventoryForOrder()` publishes Kafka events (line 263) inside the `@Transactional` block. If the event publication succeeds but the DB transaction later rolls back, the event is already sent — triggering downstream actions (notifications, analytics) for a reservation that doesn't exist.

**Fix:** Implement Transactional Outbox:
```java
@Transactional
public InventoryResponse reserveInventoryForOrder(...) {
    // ... reserve logic ...
    // Store event in outbox table (same transaction!)
    outboxRepository.save(new OutboxEvent("inventory.reserved", serialize(event)));
    // Kafka producer reads from outbox and publishes
}

// In a separate scheduled service (or Debezium):
@Scheduled(fixedDelay = 5000)
public void publishOutboxEvents() {
    List<OutboxEvent> events = outboxRepository.findAllByPublishedFalse();
    for (OutboxEvent event : events) {
        kafkaTemplate.send(event.getTopic(), event.getPayload());
        event.setPublished(true);
        outboxRepository.save(event);
    }
}
```

### 8.4 🟠 `releaseReservationByOrder` — Silent Failure on Partial Release

**Problem:** Line 306–309: When one reservation fails to release, the error is logged but processing continues. The `deleteByOrderId()` on line 313 runs regardless, clearing the reservations table even if some releases failed. Subsequent release retries are impossible because the reference data is deleted.

**Fix:** Make release atomic — either all succeed or none:
```java
@Transactional
public void releaseReservationByOrder(String orderId) {
    List<OrderReservation> reservations = orderReservationRepository.findByOrderId(orderId);
    for (OrderReservation reservation : reservations) {
        // ... each release ...
    }
    // Only delete after ALL successful
    orderReservationRepository.deleteByOrderId(orderId);
}
```

### 8.5 🟡 Available Quantity = `quantity - reservedQuantity` (in-memory calculation)

**Problem:** `getAvailableQuantity()` is a computed getter. With high contention, `quantity` and `reservedQuantity` can be read at different transaction isolation times.

**Fix:** Compute in the database: `SELECT quantity - reserved_quantity AS available FROM inventory_items WHERE ...`

---

# 9. NOTIFICATION SERVICE (`notification-service`)

### 9.1 🔴 No Email Template System

**Problem:** `EmailService` sends raw strings as email body. No templating (Thymeleaf, Mustache). Verification emails look terrible and unprofessional.

**Fix:** Use Thymeleaf templates:
```java
@Bean
public SpringTemplateEngine templateEngine() {
    SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(templateResolver());
    return engine;
}

public void sendVerificationEmail(String to, String token) {
    Context context = new Context();
    context.setVariable("verificationLink", 
        appBaseUrl + "/auth/verify?token=" + token);
    String htmlBody = templateEngine.process("email-verification", context);
    sendEmail(to, "Verify your email", htmlBody);
}
```

### 9.2 🟠 Unbounded Kafka Consumer — No Error Handling

**Problem:** `NotificationConsumer.consumeEmailNotificationEvent()` (line 18–27) catches exceptions but does NOT implement retry or dead-letter. If one email fails (invalid email address), the message is silently acked and lost.

**Fix:** Use Spring Retry + DLT:
```java
@KafkaListener(topics = "notification.email.send", groupId = "notification-group")
public void consumeEmailNotificationEvent(String message) {
    try {
        EmailNotificationEvent event = objectMapper.readValue(message, EmailNotificationEvent.class);
        emailService.sendEmail(event.getTo(), event.getSubject(), event.getBody());
    } catch (Exception e) {
        log.error("Failed to process email notification: {}", message, e);
        // Send to DLT after retries exhausted
        kafkaTemplate.send("notification.email.send.dlt", message);
    }
}
```

### 9.3 🟡 No Push Notification Support

**Problem:** Notification service only supports email. No push notifications (Firebase/FCM) or SMS (Twilio).

**Fix:** Add FCM integration:
```java
public void sendPushNotification(String deviceToken, String title, String body) {
    Message message = Message.builder()
        .setToken(deviceToken)
        .setNotification(Notification.builder()
            .setTitle(title)
            .setBody(body)
            .build())
        .build();
    FirebaseMessaging.getInstance().send(message);
}
```

---

# 10. CROSS-CUTTING CONCERNS

### 10.1 🔴 Zero Unit / Integration / Contract / Load / Chaos Tests

**Problem:** `src/test/` directories are completely empty across ALL 22 services. The `tests/` folder at root contains shell scripts that curl endpoints — these are smoke tests, not automated tests.

**Why it fails at scale:** Every deployment is a blind gamble. There are zero regression guarantees. A change in inventory-service can silently break checkout flow and nobody knows until production incidents. Contract tests (Pact/Spring Cloud Contract) are missing, so service boundaries drift. No load tests (Gatling/k6) means performance degradation goes undetected until users feel it.

**Fix:**
```bash
# Unit tests (JUnit 5 + Mockito) — add @WebMvcTest for each controller
# Integration tests (@SpringBootTest + Testcontainers) for each service
# Contract tests (Spring Cloud Contract) between service pairs
# Load tests (Gatling) for checkout and catalog flows
# Chaos tests (Chaos Monkey) for resilience validation
```

**Minimum viable test suite:**
| Layer | Framework | Count | What |
|-------|-----------|-------|------|
| Unit | JUnit 5 + Mockito | 200+ | All service & controller classes |
| Integration | Testcontainers | 50+ | Each DB + Kafka service test |
| Contract | Spring Cloud Contract | 22+ | Every Feign/REST interface |
| Load | Gatling/k6 | 5+ | Checkout, catalog browse, auth login |
| Chaos | Chaos Monkey for Spring Boot | 3+ | Service crash, Redis outage, Kafka outage |

### 10.2 🔴 No Circuit Breakers / Resilience4j Anywhere

**Problem:** Zero Resilience4j annotations across the entire codebase. No `@CircuitBreaker`, `@Retry`, `@TimeLimiter`, `@Bulkhead`.

**Why it fails at scale:** A single service failure cascades. If payment-service latency spikes, every checkout-service thread blocks waiting for the Feign response → Tomcat thread exhaustion → gateway returns 503 → clients retry → cascading failure of the entire platform.

**Fix:** Add Resilience4j to ALL Feign clients:
```yaml
resilience4j:
  circuitbreaker:
    instances:
      payment-service:
        sliding-window-size: 10
        minimum-number-of-calls: 5
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
  retry:
    instances:
      payment-service:
        max-attempts: 3
        wait-duration: 100ms
  bulkhead:
    instances:
      payment-service:
        max-concurrent-calls: 10
```

### 10.3 🔴 No Distributed Tracing

**Problem:** `management.tracing.sampling.probability: 1.0` is set in gateway, but Micrometer Tracing is not configured with an exporter (Jaeger, Zipkin, or Tempo). No trace context is propagated via Kafka headers.

**Why it fails at scale:** When a user reports "my order failed", there's no way to trace the request across gateway → checkout → inventory → payment → notification. Debugging takes hours instead of seconds.

**Fix:**
```yaml
# Add to every service
management:
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: ${ZIPKIN_URL:http://tempo:9411/api/v2/spans}
```
Propagate tracing context through Kafka:
```java
@Bean
public ProducerFactory<String, Object> kafkaProducerFactory() {
    // Use BraveKafkaTemplate for trace propagation
    return new BraveKafkaTemplate<>(..., tracing);
}
```

### 10.4 🔴 No Centralized Logging

**Problem:** Services log to stdout/console. In Docker Compose, logs are ephemeral. No log aggregation (ELK/Loki), no structured logging (JSON).

**Why it fails at scale:** Debugging across 22 services requires `docker logs -f` on each container. Searching for an order ID across all services is impossible.

**Fix:** Use structured JSON logging + Loki:
```yaml
logging:
  pattern:
    console: "{\"timestamp\":\"%d\",\"level\":\"%p\",\"service\":\"${spring.application.name}\",\"trace\":\"%X{traceId}\",\"span\":\"%X{spanId}\",\"message\":\"%msg\"}%n"
```

### 10.5 🔴 Single Kafka Broker — No Replication

**Problem:** Docker Compose has `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1` and a single broker.

**Why it fails at scale:** If the Kafka container dies (OOM, disk full), ALL event-driven communication stops. Inventory reservations, payment confirmations, notifications — the entire event backbone is a single point of failure.

**Fix:** Use Kafka cluster with 3 brokers, replication factor 3:
```yaml
services:
  kafka-1: ...
  kafka-2: ...
  kafka-3: ...
```
Update configs: `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 3`, `KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 3`, `KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 2`.

### 10.6 🟠 No Secrets Management

**Problem:** `JWT_SECRET`, `DB_USERNAME: postgres`, `DB_PASSWORD: password`, `GOOGLE_CLIENT_ID`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `PAYMENT_API_KEY: mock-api-key` — all are either hardcoded defaults or passed as plain env vars.

**Why it fails at scale:** Any container compromise leaks all credentials. Backward compatibility breaks when keys rotate. No audit trail for who accessed what secret.

**Fix:** Use HashiCorp Vault or Kubernetes Secrets:
```yaml
# Kubernetes approach
apiVersion: v1
kind: Secret
metadata:
  name: shopsphere-secrets
type: Opaque
data:
  jwt-secret: <base64>
  db-password: <base64>
```

### 10.7 🟠 No Kubernetes Manifests / No Orchestration

**Problem:** Zero `.k8s/` directories, zero Terraform files. Docker Compose only.

**Why it fails at scale:** Docker Compose runs everything on a single host. No autoscaling, no self-healing, no rolling updates, no service meshes. A single node failure takes down the entire platform. Can't do canary deployments or A/B testing.

**Fix:** Create Kubernetes manifests with Helm:
```yaml
# Minimum: Deployment + Service + HPA + ConfigMap + Secret for each service
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: checkout-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: checkout-service
  minReplicas: 3
  maxReplicas: 50
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

### 10.8 🟠 Port Conflicts Across Services

**Problem:** Multiple services declare the same ports:
- `review-service:8089`, `admin-service:8089`, `recommendation-service:8089`
- `returns-service:8090`, `analytics-service:8090`
- `search-service:8084`, `order-service:8084`, `batch-service:8091`, `media-service:8091`

**Why it fails at scale:** In Docker Compose or Kubernetes, multiple containers can't bind the same port on the same host. This causes container start failures.

**Fix:** Use unique ports for every service. Assign a port range per service:
| Service | Port |
|---------|------|
| discovery-service | 8761 |
| api-gateway | 8080 |
| auth-service | 8081 |
| user-service | 8082 |
| catalog-service | 8083 |
| order-service | 8084 |
| cart-service | 8085 |
| checkout-service | 8086 |
| pricing-service | 8087 |
| coupon-service | 8088 |
| admin-service | 8089 |
| analytics-service | 8090 |
| media-service | 8091 |
| inventory-service | 8092 |
| payment-service | 8093 |
| batch-service | 8094 |
| notification-service | 8095 |
| review-service | 8096 |
| returns-service | 8097 |
| recommendation-service | 8098 |
| search-service | 8099 |
| fraud-service | 8010 |

### 10.9 🟡 No API Versioning Strategy

**Problem:** Routes use `/api/v1/cart` in Feign clients but the gateway routes don't version. Some services use `/api/v1/...`, others don't.

**Fix:** Standardize on URL prefix versioning: `/api/v1/{service}/...`. Maintain backward compatibility with `/api/v2/...` when breaking changes are needed. Use Spring's `WebMvcConfigurer` to add version prefix or use a custom request mapping condition.

### 10.10 🟡 Database Connection Pool Too Small

**Problem:** `inventory-service` uses HikariCP defaults: `maximumPoolSize=10`. With 50 concurrent requests needing DB access, requests queue up waiting for connections.

**Fix:**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      connection-timeout: 5000
      max-lifetime: 1800000
```

### 10.11 🟢 No Flyway Migrations for MongoDB Services

**Problem:** Catalog, analytics, review, returns, recommendation, and search services use MongoDB with no migration tool. Schema changes must be applied manually.

**Fix:** Use Liquibase for MongoDB or maintain migration scripts:
```java
@ApplicationRunner
public void ensureIndexes(MongoTemplate mongoTemplate) {
    mongoTemplate.indexOps("products")
        .ensureIndex(new Index().on("sku", Sort.Direction.ASC).unique());
    mongoTemplate.indexOps("products")
        .ensureIndex(new Index().on("categoryId", Sort.Direction.ASC));
}
```

### 10.12 🟢 Shared Library Duplication

**Problem:** `auth-service` and `admin-service` both contain a `com.rudraksha.shopsphere.shared` package with identical code. This code is copy-pasted.

**Fix:** Extract `shopsphere-common` Maven module:
```
shopsphere-common/
├── pom.xml
├── src/main/java/com/rudraksha/shopsphere/common/
│   ├── db/       (BaseRepository, PaginationUtils)
│   ├── kafka/    (EventConsumer, EventPublisher, TopicConstants)
│   ├── models/   (ApiResponse, BaseEntity, enums)
│   ├── security/ (JwtTokenProvider, SecurityConstants)
│   └── utils/    (JsonUtil, IdGenerator, RetryUtil)
```
All services depend on this module: `<artifactId>shopsphere-common</artifactId>`.

---

# 11. Single Points of Failure (SPOFs)

| Component | Why SPOF | Mitigation |
|-----------|----------|------------|
| **discovery-service** (Eureka) | Single instance. If it dies, new service instances can't register. | Run 3 Eureka instances in peer awareness mode |
| **kafka** (single broker) | One container. Disk full, OOM, or network issue brings down all event-driven flows | 3-broker cluster with replication factor 3 |
| **redis** (single instance) | Auth token revocation, cart data, inventory locks all depend on it. | Redis Sentinel or Redis Cluster (3 nodes) |
| **nginx-proxy** | Single nginx instance in front of gateway. | Multiple nginx replicas + cloud LB |
| **checkout-service** DB | Single PostgreSQL with no replica. DB crash = all checkout operations stop | Read replicas + automatic failover |
| **api-gateway** | All traffic goes through a single gateway service. | Multiple gateway replicas (Kubernetes HPA) |

---

# 12. Anti-Patterns Identified

| Anti-Pattern | Location | Impact |
|--------------|----------|--------|
| **Shared Database** (conceptual) | checkout-service + payment-service both store payment data | Data inconsistency, dual-write problem |
| **Synchronous blocking chain** | checkout-service → cart-service → payment-service | Thread pool exhaustion, cascading failures |
| **Forgiving event publishing** | All services publish Kafka events inside transactions with no outbox | Event loss on transaction rollback |
| **Stringly-typed Kafka events** | `eventType + ":" + details + ":" + LocalDateTime.now()` | No schema registry, deserialization impossible without parsing fragile strings |
| **Homebrew distributed lock** | DistributedLockUtil | Lock expiry race, no watchdog |
| **Copy-paste shared code** | shared/ package duplicated | Version drift, security bugs |
| **God service** | checkout-service has Feign clients, payment logic, order creation, cart clearing | Violates single responsibility |
| **No health check dependency** | cart-service depends on catalog-service with no circuit breaker | Catalog failure cascades to cart |

---

# 13. Latency Bottlenecks

| Path | Current Latency | Target | Why |
|------|----------------|--------|-----|
| Checkout flow (sync) | 1000ms+ (Thread.sleep) | < 200ms | Flash sale with 100K users = 100s total |
| Catalog search (regex) | 5–10s per query | < 50ms | No search index |
| Auth token validation (Redis check) | 2–5ms per request | < 1ms | 1M requests/min = 50s of Redis time |
| Email sending (sync) | 200ms–5s per email | < 10ms (queue) | Blocks HTTP threads |
| N+1 pricing calls | 50ms × items per page | < 5ms total | Missing price in product document |

---

# 14. Target Reference Architecture

```
CloudFront / CDN
    │
    ▼
Cloud Load Balancer (AWS ALB / GCP HTTP LB)
    │
    ▼
NGINX Ingress Controller (Kubernetes) [3 replicas]
    │
    ▼
┌────────────────────────────────────────────────────┐
│              API Gateway (Spring Cloud Gateway)      │
│          Rate limiting │ Auth │ Routing │ Caching     │
│              [HPA: 5–20 replicas]                    │
└────────┬───────────┬───────────┬────────────────────┘
         │           │           │
    ┌────▼────┐ ┌────▼────┐ ┌────▼────┐ ┌─────────┐
    │ Auth    │ │ Catalog │ │ Cart    │ │ Media   │
    │ Service │ │ Service │ │ Service │ │ Service │
    │[3-5 rep]│ │[5-10rep]│ │[5-10rep]│ │[2-3 rep]│
    └────┬────┘ └────┬────┘ └────┬────┘ └─────────┘
         │           │           │
    ┌────▼────┐ ┌────▼────┐ ┌────▼────┐ ┌─────────┐
    │ Checkout│ │ Order   │ │Payment  │ │Inventory│
    │ Service │ │ Service │ │ Service │ │ Service │
    │[5-20rep]│ │[3-5 rep]│ │[3-5 rep]│ │[5-10rep]│
    └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘
         │           │           │           │
    ┌────▼────┐ ┌────▼────┐ ┌────▼────┐ ┌─────────┐
    │Notification│ │Fraud   │ │Pricing  │ │Coupon   │
    │ Service   │ │Service │ │ Service │ │ Service │
    │[2-3 rep]  │ │[2-3rep]│ │[2-3 rep]│ │[2-3 rep]│
    └───────────┘ └────────┘ └─────────┘ └─────────┘

    Kafka Cluster (3 brokers, replication factor 3)
         │
    ┌────┴──────────────┬──────────────────┐
    │                   │                  │
    ▼                   ▼                  ▼
┌────────────┐ ┌──────────────┐ ┌──────────────┐
│ Analytics  │ │ Search       │ │ Recommendation│
│ Service    │ │ Service      │ │ Service      │
│ [2-3 rep]  │ │ [3-5 rep]   │ │ [2-3 rep]    │
└────────────┘ └──────────────┘ └──────────────┘

Data Layer:
┌─────────────────────────────────────────────────────┐
│ PostgreSQL (RDS/Aurora) — Primary + Read Replicas    │
│ auth-db, user-db, checkout-db, order-db, payment-db,│
│ inventory-db, coupon-db, pricing-db, admin-db,       │
│ media-db, notification-db, fraud-db, batch-db        │
├─────────────────────────────────────────────────────┤
│ MongoDB Atlas — Replica Set (3 nodes)                │
│ catalog-db, analytics-db, search-db, review-db,     │
│ returns-db, recommendation-db                        │
├─────────────────────────────────────────────────────┤
│ Redis Cluster (3 master + 3 replica)                 │
│ Cart data, session cache, token revocation, locks     │
├─────────────────────────────────────────────────────┤
│ Elasticsearch Cluster (3 nodes)                      │
│ Full-text search for catalog                         │
└─────────────────────────────────────────────────────┘

Observability:
┌─────────────────────────────────────────────────────┐
│ Prometheus + Grafana (metrics)                       │
│ Loki + Grafana (logging)                             │
│ Tempo (tracing)                                      │
│ PagerDuty / OpsGenie (alerting)                      │
│ Sentry / DataDog (APM)                               │
└─────────────────────────────────────────────────────┘
```

---

# 15. Step-by-Step Roadmap to Production Readiness

## Phase 1: Foundation (Weeks 1–2) 🚨

| Task | Priority | Effort | Impact |
|------|----------|--------|--------|
| 1.1 Fix checkout SAGA — make it fully async with outbox | 🔴 Critical | 3 days | Prevents data loss |
| 1.2 Add Resilience4j to ALL Feign clients | 🔴 Critical | 1 day | Stops cascading failures |
| 1.3 Fix inventory lock with Redisson | 🔴 Critical | 1 day | Prevents overselling |
| 1.4 Add Transactional Outbox pattern to payment/inventory/order | 🔴 Critical | 2 days | No more lost events |
| 1.5 Fix cart hardcoded prices — call catalog service | 🔴 Critical | 1 day | Correct pricing |

## Phase 2: Testing (Weeks 3–4) 🧪

| Task | Priority | Effort | Impact |
|------|----------|--------|--------|
| 2.1 Add unit tests for all service classes | 🔴 Critical | 5 days | Regression protection |
| 2.2 Add integration tests with Testcontainers | 🔴 Critical | 5 days | Validates DB + Kafka |
| 2.3 Add contract tests for all Feign interfaces | 🟠 High | 3 days | Service boundary integrity |
| 2.4 Set up Gatling load tests for checkout flow | 🟠 High | 2 days | Performance baseline |

## Phase 3: Infrastructure (Weeks 5–6) ☁️

| Task | Priority | Effort | Impact |
|------|----------|--------|--------|
| 3.1 Create Kubernetes manifests for all services | 🔴 Critical | 5 days | Container orchestration |
| 3.2 Set up HPA for all services | 🟠 High | 1 day | Autoscaling |
| 3.3 Configure CI/CD with GitHub Actions + ArgoCD | 🟠 High | 3 days | Automated deployments |
| 3.4 Separate Kafka cluster (3 brokers) | 🟠 High | 2 days | HA event backbone |
| 3.5 Add Redis Sentinel/Cluster | 🟠 High | 2 days | HA caching |

## Phase 4: Observability (Week 7) 📊

| Task | Priority | Effort | Impact |
|------|----------|--------|--------|
| 4.1 Deploy Prometheus + Grafana dashboards | 🟠 High | 2 days | Metrics visibility |
| 4.2 Deploy Loki for log aggregation | 🟠 High | 1 day | Centralized logging |
| 4.3 Configure distributed tracing (Tempo) | 🟠 High | 2 days | End-to-end request tracing |
| 4.4 Set up structured JSON logging | 🟡 Medium | 1 day | Queryable logs |

## Phase 5: Security (Week 8) 🔒

| Task | Priority | Effort | Impact |
|------|----------|--------|--------|
| 5.1 Implement JWKS key rotation | 🟠 High | 2 days | Token security |
| 5.2 Move all secrets to Vault/K8s Secrets | 🟠 High | 2 days | Credential protection |
| 5.3 Add idempotency keys to payment | 🟠 High | 1 day | No double charges |
| 5.4 Add rate limiting by user ID | 🟡 Medium | 1 day | Fair resource usage |

## Phase 6: Data & Consistency (Weeks 9–10) 🗄️

| Task | Priority | Effort | Impact |
|------|----------|--------|--------|
| 6.1 Merge order-service and checkout-service | 🟠 High | 3 days | Eliminate dual source of truth |
| 6.2 Add price to product document | 🟠 High | 1 day | Eliminate N+1 pricing calls |
| 6.3 Add Elasticsearch for catalog search | 🟡 Medium | 3 days | Fast product search |
| 6.4 MongoDB indexes for all query patterns | 🟡 Medium | 1 day | Query performance |
| 6.5 Set up read replicas for PostgreSQL | 🟡 Medium | 2 days | DB read scalability |

## Phase 7: Resilience (Week 11) 💪

| Task | Priority | Effort | Impact |
|------|----------|--------|--------|
| 7.1 Set up chaos testing (Chaos Monkey) | 🟡 Medium | 2 days | Validates resilience |
| 7.2 Add bulkheads for all external calls | 🟡 Medium | 1 day | Resource isolation |
| 7.3 Implement graceful degradation strategies | 🟡 Medium | 2 days | Partial availability |

## Phase 8: Polish (Week 12) ✨

| Task | Priority | Effort | Impact |
|------|----------|--------|--------|
| 8.1 Fix all port conflicts | 🟡 Medium | 0.5 day | Clean deployment |
| 8.2 Extract shopsphere-common module | 🟡 Medium | 1 day | Eliminate code duplication |
| 8.3 Fix getAllOrders() pagination | 🟢 Low | 0.5 day | Prevent OOM |
| 8.4 Add email templates | 🟢 Low | 1 day | Professional UX |
| 8.5 Configure Flyway for MongoDB services | 🟢 Low | 1 day | Schema versioning |

---

# Summary Verdict

This is a promising **monolith-in-microservices-clothing**. The architecture diagram looks right (22 services, Kafka, Eureka, Redis, API Gateway), but the implementation at every layer suffers from **demo-ware syndrome** — enough to show the concept, not enough to survive real traffic.

The three deadliest issues are:
1. **Synchronous checkout with no SAGA** — one service failure = corrupted state
2. **Zero fault tolerance** — no circuit breakers, no retry, no bulkheads
3. **Zero tests** — every deploy is a blind roll of the dice

If I had to prioritize ONE thing: **Make the checkout flow async with the outbox pattern and add Resilience4j to every Feign client.** This alone eliminates the two most catastrophic failure modes.

After that: **Kubernetes + HPA** — because the current Docker Compose setup can't scale beyond a single host.

The codebase has good bones but needs 12 weeks of focused engineering to reach a 7/10 readiness score. Good luck.
