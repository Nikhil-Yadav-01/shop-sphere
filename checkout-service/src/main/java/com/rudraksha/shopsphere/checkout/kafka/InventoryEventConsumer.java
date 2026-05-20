package com.rudraksha.shopsphere.checkout.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rudraksha.shopsphere.checkout.event.EventEnvelope;
import com.rudraksha.shopsphere.checkout.event.InventoryReservationFailedPayload;
import com.rudraksha.shopsphere.checkout.event.InventoryReservedPayload;
import com.rudraksha.shopsphere.checkout.service.CheckoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Consumes inventory SAGA events from Kafka and delegates to CheckoutService.
 * Implements consumer-side idempotency via Redis to deduplicate redelivered messages.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {

    private static final String IDEMPOTENCY_PREFIX = "checkout:processed:inventory:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final CheckoutService checkoutService;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    @KafkaListener(topics = "inventory.reserved", groupId = "checkout-service-inventory",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onInventoryReserved(String message, Acknowledgment ack) {
        log.info("Received inventory.reserved event in checkout-service");
        try {
            EventEnvelope<InventoryReservedPayload> envelope = objectMapper.readValue(message,
                    new TypeReference<>() {});

            if (isDuplicate(envelope.getId())) {
                log.debug("Duplicate inventory.reserved event ignored: {}", envelope.getId());
                ack.acknowledge();
                return;
            }

            // Only handle events that carry a sessionId (SAGA context)
            if (envelope.getPayload().getSessionId() == null) {
                log.debug("Skipping inventory.reserved without sessionId (non-SAGA event)");
                ack.acknowledge();
                return;
            }

            checkoutService.handleInventoryReserved(envelope.getPayload());
            markProcessed(envelope.getId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing inventory.reserved in checkout-service: {}", message, e);
            // Do NOT ack — Kafka will redeliver based on retry policy
        }
    }

    @KafkaListener(topics = "inventory.reservation.failed", groupId = "checkout-service-inventory",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onInventoryReservationFailed(String message, Acknowledgment ack) {
        log.info("Received inventory.reservation.failed event in checkout-service");
        try {
            EventEnvelope<InventoryReservationFailedPayload> envelope = objectMapper.readValue(message,
                    new TypeReference<>() {});

            if (isDuplicate(envelope.getId())) {
                log.debug("Duplicate inventory.reservation.failed event ignored: {}", envelope.getId());
                ack.acknowledge();
                return;
            }

            if (envelope.getPayload().getSessionId() == null) {
                log.debug("Skipping inventory.reservation.failed without sessionId (non-SAGA event)");
                ack.acknowledge();
                return;
            }

            checkoutService.handleInventoryReservationFailed(envelope.getPayload());
            markProcessed(envelope.getId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing inventory.reservation.failed in checkout-service: {}", message, e);
        }
    }

    private boolean isDuplicate(String eventId) {
        String key = IDEMPOTENCY_PREFIX + eventId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private void markProcessed(String eventId) {
        String key = IDEMPOTENCY_PREFIX + eventId;
        redisTemplate.opsForValue().set(key, "1", IDEMPOTENCY_TTL);
    }
}
