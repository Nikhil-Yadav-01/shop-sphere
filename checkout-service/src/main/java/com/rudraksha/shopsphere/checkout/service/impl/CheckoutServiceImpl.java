package com.rudraksha.shopsphere.checkout.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rudraksha.shopsphere.checkout.client.CartClient;
import com.rudraksha.shopsphere.checkout.dto.request.CheckoutRequest;
import com.rudraksha.shopsphere.checkout.dto.response.CheckoutSessionResponse;
import com.rudraksha.shopsphere.checkout.entity.CheckoutSession;
import com.rudraksha.shopsphere.checkout.entity.OutboxEvent;
import com.rudraksha.shopsphere.checkout.event.*;
import com.rudraksha.shopsphere.checkout.exception.CheckoutException;
import com.rudraksha.shopsphere.checkout.repository.CheckoutSessionRepository;
import com.rudraksha.shopsphere.checkout.repository.OutboxEventRepository;
import com.rudraksha.shopsphere.checkout.service.CheckoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutServiceImpl implements CheckoutService {

    private static final String SOURCE = "checkout-service";

    private final CartClient cartClient;
    private final CheckoutSessionRepository sessionRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    // ── Public API ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CheckoutSessionResponse initiateCheckout(String userId, CheckoutRequest request, String idempotencyKey) {
        log.info("Initiating checkout for userId={}, idempotencyKey={}", userId, idempotencyKey);

        // 1. Idempotency: if already processed, return existing session
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = sessionRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Idempotent checkout request detected. Returning existing session: {}",
                        existing.get().getSessionId());
                return toResponse(existing.get());
            }
        }

        // 2. Get cart items (circuit-breaker-protected via Feign fallback)
        CartClient.CartResponse cart = cartClient.getCart(userId);
        if (cart == null || cart.items() == null || cart.items().isEmpty()) {
            throw new CheckoutException("Cart is empty or unavailable for userId=" + userId,
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // 3. Compute totals
        BigDecimal subtotal = cart.totalPrice();
        BigDecimal taxAmount = subtotal.multiply(BigDecimal.valueOf(0.08));
        BigDecimal totalAmount = subtotal.add(taxAmount).add(BigDecimal.valueOf(9.99)); // +shipping flat rate

        // 4. Build shipping/billing address strings
        String shippingAddr = request.getShippingAddress().getAddressLine1() + ", "
                + request.getShippingAddress().getCity() + ", "
                + request.getShippingAddress().getState() + " "
                + request.getShippingAddress().getPostalCode();
        String billingAddr = request.getShippingAddress().getAddressLine1();

        // 5. Persist checkout session
        String sessionId = UUID.randomUUID().toString();
        CheckoutSession session = CheckoutSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .status(CheckoutSession.CheckoutStatus.PENDING)
                .totalAmount(totalAmount)
                .idempotencyKey(idempotencyKey)
                .build();
        sessionRepository.save(session);

        // 6. Build event payload
        List<CheckoutInitiatedPayload.OrderItemPayload> items = cart.items().stream()
                .map(item -> CheckoutInitiatedPayload.OrderItemPayload.builder()
                        .productId(item.productId())
                        .productName(item.productName())
                        .quantity(item.quantity())
                        .unitPrice(item.price())
                        .totalPrice(item.subtotal())
                        .build())
                .collect(Collectors.toList());

        CheckoutInitiatedPayload payload = CheckoutInitiatedPayload.builder()
                .sessionId(sessionId)
                .userId(userId)
                .totalAmount(totalAmount)
                .taxAmount(taxAmount)
                .shippingAddress(shippingAddr)
                .billingAddress(billingAddr)
                .items(items)
                .build();

        EventEnvelope<CheckoutInitiatedPayload> envelope = EventEnvelope.<CheckoutInitiatedPayload>builder()
                .type("checkout.initiated")
                .source(SOURCE)
                .payload(payload)
                .build();

        // 7. Persist outbox event (in same transaction — atomicity guaranteed)
        saveOutboxEvent(sessionId, "checkout.initiated", "checkout.initiated", sessionId, envelope);

        log.info("Checkout session created: sessionId={}, userId={}", sessionId, userId);
        return toResponse(session);
    }

    @Override
    @Transactional(readOnly = true)
    public CheckoutSessionResponse getCheckoutSession(String sessionId) {
        CheckoutSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new CheckoutException(
                        "Checkout session not found: " + sessionId, HttpStatus.NOT_FOUND));
        return toResponse(session);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CheckoutSessionResponse> getUserSessions(String userId, Pageable pageable) {
        return sessionRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    // ── SAGA Callbacks ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void handleInventoryReserved(InventoryReservedPayload payload) {
        log.info("Handling inventory.reserved: sessionId={}, orderNumber={}",
                payload.getSessionId(), payload.getOrderNumber());
        sessionRepository.findBySessionId(payload.getSessionId()).ifPresentOrElse(
                session -> {
                    if (session.getStatus() == CheckoutSession.CheckoutStatus.ORDER_CREATED
                            || session.getStatus() == CheckoutSession.CheckoutStatus.PENDING) {
                        session.setStatus(CheckoutSession.CheckoutStatus.INVENTORY_RESERVED);
                        session.setOrderNumber(payload.getOrderNumber());
                        sessionRepository.save(session);
                        log.info("Session {} advanced to INVENTORY_RESERVED", payload.getSessionId());
                    }
                },
                () -> log.warn("Session not found for inventory.reserved: {}", payload.getSessionId())
        );
    }

    @Override
    @Transactional
    public void handleInventoryReservationFailed(InventoryReservationFailedPayload payload) {
        log.warn("Handling inventory.reservation.failed: sessionId={}, reason={}",
                payload.getSessionId(), payload.getReason());
        sessionRepository.findBySessionId(payload.getSessionId()).ifPresentOrElse(
                session -> {
                    session.setStatus(CheckoutSession.CheckoutStatus.FAILED);
                    session.setFailureReason("Inventory reservation failed: " + payload.getReason());
                    sessionRepository.save(session);

                    CheckoutFailedPayload failedPayload = CheckoutFailedPayload.builder()
                            .sessionId(payload.getSessionId())
                            .orderNumber(payload.getOrderNumber())
                            .userId(payload.getUserId())
                            .reason("Inventory reservation failed: " + payload.getReason())
                            .build();
                    EventEnvelope<CheckoutFailedPayload> failEnvelope = EventEnvelope.<CheckoutFailedPayload>builder()
                            .type("checkout.failed")
                            .source(SOURCE)
                            .payload(failedPayload)
                            .build();
                    saveOutboxEvent(payload.getSessionId(), "checkout.failed",
                            "checkout.failed", payload.getSessionId(), failEnvelope);
                    log.info("Session {} marked FAILED due to inventory reservation failure", payload.getSessionId());
                },
                () -> log.warn("Session not found for inventory.reservation.failed: {}", payload.getSessionId())
        );
    }

    @Override
    @Transactional
    public void handlePaymentSucceeded(PaymentSucceededPayload payload) {
        log.info("Handling payment.succeeded: sessionId={}, orderNumber={}",
                payload.getSessionId(), payload.getOrderNumber());
        sessionRepository.findBySessionId(payload.getSessionId()).ifPresentOrElse(
                session -> {
                    session.setStatus(CheckoutSession.CheckoutStatus.COMPLETED);
                    session.setOrderNumber(payload.getOrderNumber());
                    sessionRepository.save(session);

                    // Clear cart (best-effort — fallback logs but doesn't fail)
                    try {
                        cartClient.clearCart(payload.getUserId());
                        log.info("Cart cleared for userId={} after successful payment", payload.getUserId());
                    } catch (Exception e) {
                        log.error("Failed to clear cart for userId={} (will not retry inline)",
                                payload.getUserId(), e);
                    }

                    // Notify downstream (e.g., notification-service) via outbox
                    CheckoutCompletedPayload completedPayload = CheckoutCompletedPayload.builder()
                            .sessionId(payload.getSessionId())
                            .orderNumber(payload.getOrderNumber())
                            .userId(payload.getUserId())
                            .totalAmount(payload.getAmount())
                            .build();
                    EventEnvelope<CheckoutCompletedPayload> envelope = EventEnvelope.<CheckoutCompletedPayload>builder()
                            .type("checkout.completed")
                            .source(SOURCE)
                            .payload(completedPayload)
                            .build();
                    saveOutboxEvent(payload.getSessionId(), "checkout.completed",
                            "checkout.completed", payload.getSessionId(), envelope);
                    log.info("Session {} COMPLETED", payload.getSessionId());
                },
                () -> log.warn("Session not found for payment.succeeded: {}", payload.getSessionId())
        );
    }

    @Override
    @Transactional
    public void handlePaymentFailed(PaymentFailedPayload payload) {
        log.warn("Handling payment.failed: sessionId={}, reason={}",
                payload.getSessionId(), payload.getReason());
        sessionRepository.findBySessionId(payload.getSessionId()).ifPresentOrElse(
                session -> {
                    session.setStatus(CheckoutSession.CheckoutStatus.FAILED);
                    session.setFailureReason("Payment failed: " + payload.getReason());
                    sessionRepository.save(session);

                    CheckoutFailedPayload failedPayload = CheckoutFailedPayload.builder()
                            .sessionId(payload.getSessionId())
                            .orderNumber(payload.getOrderNumber())
                            .userId(payload.getUserId())
                            .reason("Payment failed: " + payload.getReason())
                            .build();
                    EventEnvelope<CheckoutFailedPayload> failEnvelope = EventEnvelope.<CheckoutFailedPayload>builder()
                            .type("checkout.failed")
                            .source(SOURCE)
                            .payload(failedPayload)
                            .build();
                    saveOutboxEvent(payload.getSessionId(), "checkout.failed",
                            "checkout.failed", payload.getSessionId(), failEnvelope);
                    log.info("Session {} marked FAILED due to payment failure", payload.getSessionId());
                },
                () -> log.warn("Session not found for payment.failed: {}", payload.getSessionId())
        );
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private <T> void saveOutboxEvent(String aggregateId, String eventType, String topic, String key,
                                     EventEnvelope<T> envelope) {
        try {
            String json = objectMapper.writeValueAsString(envelope);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .topic(topic)
                    .messageKey(key)
                    .payload(json)
                    .build();
            outboxRepository.save(outboxEvent);
            log.debug("Outbox event saved: type={}, aggregateId={}", eventType, aggregateId);
        } catch (JsonProcessingException e) {
            throw new CheckoutException("Failed to serialize outbox event: " + eventType,
                    HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    private CheckoutSessionResponse toResponse(CheckoutSession session) {
        return CheckoutSessionResponse.builder()
                .sessionId(session.getSessionId())
                .status(session.getStatus().name())
                .orderNumber(session.getOrderNumber())
                .totalAmount(session.getTotalAmount())
                .failureReason(session.getFailureReason())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}
