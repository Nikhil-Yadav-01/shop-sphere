package com.rudraksha.shopsphere.order.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rudraksha.shopsphere.order.entity.Order;
import com.rudraksha.shopsphere.order.event.EventEnvelope;
import com.rudraksha.shopsphere.order.event.InventoryReservationFailedPayload;
import com.rudraksha.shopsphere.order.event.InventoryReservedPayload;
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
public class InventoryEventListener {

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String IDEMPOTENCY_KEY_PREFIX = "order:inventory:event:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    @KafkaListener(
            topics = "inventory.reserved",
            groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onInventoryReserved(String message, Acknowledgment ack) {
        log.info("Received inventory.reserved event in order-service: {}", message);
        try {
            EventEnvelope<InventoryReservedPayload> envelope = objectMapper.readValue(
                    message,
                    new TypeReference<EventEnvelope<InventoryReservedPayload>>() {}
            );

            String eventId = envelope.getId();
            if (Boolean.FALSE.equals(redisTemplate.opsForValue().setIfAbsent(IDEMPOTENCY_KEY_PREFIX + eventId, "PROCESSED", IDEMPOTENCY_TTL))) {
                log.warn("Event {} already processed, skipping.", eventId);
                ack.acknowledge();
                return;
            }

            InventoryReservedPayload payload = envelope.getPayload();
            if (payload != null && payload.getOrderNumber() != null) {
                String orderNumber = payload.getOrderNumber();
                orderRepository.findByOrderNumber(orderNumber).ifPresent(order -> {
                    if (order.getStatus() == Order.OrderStatus.PENDING) {
                        order.setStatus(Order.OrderStatus.PROCESSING);
                        orderRepository.save(order);
                        log.info("Order {} status updated to PROCESSING after inventory reservation.", orderNumber);
                    }
                });
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing inventory.reserved event", e);
        }
    }

    @KafkaListener(
            topics = "inventory.reservation.failed",
            groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onInventoryReservationFailed(String message, Acknowledgment ack) {
        log.info("Received inventory.reservation.failed event in order-service: {}", message);
        try {
            EventEnvelope<InventoryReservationFailedPayload> envelope = objectMapper.readValue(
                    message,
                    new TypeReference<EventEnvelope<InventoryReservationFailedPayload>>() {}
            );

            String eventId = envelope.getId();
            if (Boolean.FALSE.equals(redisTemplate.opsForValue().setIfAbsent(IDEMPOTENCY_KEY_PREFIX + eventId, "PROCESSED", IDEMPOTENCY_TTL))) {
                log.warn("Event {} already processed, skipping.", eventId);
                ack.acknowledge();
                return;
            }

            InventoryReservationFailedPayload payload = envelope.getPayload();
            if (payload != null && payload.getOrderNumber() != null) {
                String orderNumber = payload.getOrderNumber();
                orderRepository.findByOrderNumber(orderNumber).ifPresent(order -> {
                    if (order.getStatus() == Order.OrderStatus.PENDING || order.getStatus() == Order.OrderStatus.PROCESSING) {
                        order.setStatus(Order.OrderStatus.CANCELLED);
                        orderRepository.save(order);
                        log.warn("Order {} status updated to CANCELLED due to inventory reservation failure. Reason: {}", orderNumber, payload.getReason());
                    }
                });
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing inventory.reservation.failed event", e);
        }
    }
}
