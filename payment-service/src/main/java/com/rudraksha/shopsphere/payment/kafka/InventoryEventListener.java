package com.rudraksha.shopsphere.payment.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rudraksha.shopsphere.payment.dto.request.ProcessPaymentRequest;
import com.rudraksha.shopsphere.payment.entity.Payment;
import com.rudraksha.shopsphere.payment.event.EventEnvelope;
import com.rudraksha.shopsphere.payment.event.InventoryReservedPayload;
import com.rudraksha.shopsphere.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventListener {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String IDEMPOTENCY_PREFIX = "payment:event:processed:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    @KafkaListener(topics = "inventory.reserved", groupId = "payment-service-group")
    public void onInventoryReserved(String message, Acknowledgment acknowledgment) {
        log.info("Received inventory.reserved event in payment-service: {}", message);
        try {
            // 1. Deserialize EventEnvelope<InventoryReservedPayload>
            EventEnvelope<InventoryReservedPayload> envelope = objectMapper.readValue(
                    message,
                    new TypeReference<EventEnvelope<InventoryReservedPayload>>() {}
            );

            if (envelope == null || envelope.getPayload() == null) {
                log.error("Received null envelope or payload: {}", message);
                acknowledgment.acknowledge();
                return;
            }

            String eventId = envelope.getId();
            String idempotencyKey = IDEMPOTENCY_PREFIX + eventId;

            // 2. Check and set consumer idempotency via Redis
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(idempotencyKey, "processed", IDEMPOTENCY_TTL);
            if (Boolean.FALSE.equals(isNew)) {
                log.warn("Duplicate event detected, skipping: {}", eventId);
                acknowledgment.acknowledge();
                return;
            }

            InventoryReservedPayload payload = envelope.getPayload();
            String orderNumber = payload.getOrderNumber();
            String userId = payload.getUserId();
            String sessionId = payload.getSessionId();
            BigDecimal amount = payload.getTotalAmount();

            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Skipping payment for order {} with invalid or zero amount: {}", orderNumber, amount);
                acknowledgment.acknowledge();
                return;
            }

            log.info("Triggering payment processing for order {} / session {} of amount {}", orderNumber, sessionId, amount);

            ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                    .sessionId(sessionId)
                    .orderNumber(orderNumber)
                    .userId(userId)
                    .amount(amount)
                    .currency("USD")
                    .method(Payment.PaymentMethod.CREDIT_CARD)
                    .paymentMethodDetails("Mock payment details")
                    .build();

            paymentService.processPayment(request);

            // 3. Manual acknowledgment after successful processing
            acknowledgment.acknowledge();
            log.info("Successfully processed and acknowledged event: {}", eventId);

        } catch (Exception e) {
            log.error("Failed to process inventory.reserved event in payment-service", e);
            if (e instanceof com.fasterxml.jackson.core.JsonProcessingException) {
                log.error("Corrupted event payload, acknowledging to discard: {}", message);
                acknowledgment.acknowledge();
            }
        }
    }
}
