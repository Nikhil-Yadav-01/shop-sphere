package com.rudraksha.shopsphere.order.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rudraksha.shopsphere.order.entity.Order;
import com.rudraksha.shopsphere.order.event.EventEnvelope;
import com.rudraksha.shopsphere.order.event.PaymentFailedPayload;
import com.rudraksha.shopsphere.order.event.PaymentSucceededPayload;
import com.rudraksha.shopsphere.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String IDEMPOTENCY_KEY_PREFIX = "order:payment:event:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    @KafkaListener(
            topics = "payment.succeeded",
            groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentSucceeded(String message, Acknowledgment ack) {
        log.info("Received payment.succeeded event in order-service: {}", message);
        try {
            EventEnvelope<PaymentSucceededPayload> envelope = objectMapper.readValue(
                    message,
                    new TypeReference<EventEnvelope<PaymentSucceededPayload>>() {}
            );

            String eventId = envelope.getId();
            if (Boolean.FALSE.equals(redisTemplate.opsForValue().setIfAbsent(IDEMPOTENCY_KEY_PREFIX + eventId, "PROCESSED", IDEMPOTENCY_TTL))) {
                log.warn("Event {} already processed, skipping.", eventId);
                ack.acknowledge();
                return;
            }

            PaymentSucceededPayload payload = envelope.getPayload();
            if (payload != null && payload.getOrderNumber() != null) {
                String orderNumber = payload.getOrderNumber();
                orderRepository.findByOrderNumber(orderNumber).ifPresent(order -> {
                    if (order.getStatus() == Order.OrderStatus.PROCESSING || order.getStatus() == Order.OrderStatus.PENDING) {
                        order.setStatus(Order.OrderStatus.CONFIRMED);
                        orderRepository.save(order);
                        log.info("Order {} confirmed after payment success.", orderNumber);
                    }
                });
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing payment.succeeded event", e);
        }
    }

    @KafkaListener(
            topics = "payment.failed",
            groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentFailed(String message, Acknowledgment ack) {
        log.info("Received payment.failed event in order-service: {}", message);
        try {
            EventEnvelope<PaymentFailedPayload> envelope = objectMapper.readValue(
                    message,
                    new TypeReference<EventEnvelope<PaymentFailedPayload>>() {}
            );

            String eventId = envelope.getId();
            if (Boolean.FALSE.equals(redisTemplate.opsForValue().setIfAbsent(IDEMPOTENCY_KEY_PREFIX + eventId, "PROCESSED", IDEMPOTENCY_TTL))) {
                log.warn("Event {} already processed, skipping.", eventId);
                ack.acknowledge();
                return;
            }

            PaymentFailedPayload payload = envelope.getPayload();
            if (payload != null && payload.getOrderNumber() != null) {
                String orderNumber = payload.getOrderNumber();
                orderRepository.findByOrderNumber(orderNumber).ifPresent(order -> {
                    if (order.getStatus() == Order.OrderStatus.PROCESSING || order.getStatus() == Order.OrderStatus.PENDING) {
                        order.setStatus(Order.OrderStatus.CANCELLED);
                        orderRepository.save(order);
                        log.warn("Order {} cancelled after payment failure. Reason: {}", orderNumber, payload.getReason());
                    }
                });
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing payment.failed event", e);
        }
    }
}
