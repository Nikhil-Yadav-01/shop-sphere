package com.rudraksha.shopsphere.checkout.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rudraksha.shopsphere.checkout.event.EventEnvelope;
import com.rudraksha.shopsphere.checkout.event.PaymentFailedPayload;
import com.rudraksha.shopsphere.checkout.event.PaymentSucceededPayload;
import com.rudraksha.shopsphere.checkout.service.CheckoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Consumes payment SAGA events from Kafka and delegates to CheckoutService.
 * Implements consumer-side idempotency via Redis to deduplicate redelivered messages.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private static final String IDEMPOTENCY_PREFIX = "checkout:processed:payment:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final CheckoutService checkoutService;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    @KafkaListener(topics = "payment.succeeded", groupId = "checkout-service-payment",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onPaymentSucceeded(String message, Acknowledgment ack) {
        log.info("Received payment.succeeded event in checkout-service");
        try {
            EventEnvelope<PaymentSucceededPayload> envelope = objectMapper.readValue(message,
                    new TypeReference<>() {});

            if (isDuplicate(envelope.getId())) {
                log.debug("Duplicate payment.succeeded event ignored: {}", envelope.getId());
                ack.acknowledge();
                return;
            }

            checkoutService.handlePaymentSucceeded(envelope.getPayload());
            markProcessed(envelope.getId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing payment.succeeded in checkout-service: {}", message, e);
            // Do NOT ack — Kafka will redeliver based on retry policy
        }
    }

    @KafkaListener(topics = "payment.failed", groupId = "checkout-service-payment",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onPaymentFailed(String message, Acknowledgment ack) {
        log.info("Received payment.failed event in checkout-service");
        try {
            EventEnvelope<PaymentFailedPayload> envelope = objectMapper.readValue(message,
                    new TypeReference<>() {});

            if (isDuplicate(envelope.getId())) {
                log.debug("Duplicate payment.failed event ignored: {}", envelope.getId());
                ack.acknowledge();
                return;
            }

            checkoutService.handlePaymentFailed(envelope.getPayload());
            markProcessed(envelope.getId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing payment.failed in checkout-service: {}", message, e);
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
