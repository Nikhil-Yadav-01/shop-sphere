package com.rudraksha.shopsphere.inventory.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rudraksha.shopsphere.inventory.entity.OutboxEvent;
import com.rudraksha.shopsphere.inventory.event.EventEnvelope;
import com.rudraksha.shopsphere.inventory.event.InventoryReservationFailedPayload;
import com.rudraksha.shopsphere.inventory.event.OrderPlacedPayload;
import com.rudraksha.shopsphere.inventory.repository.OutboxEventRepository;
import com.rudraksha.shopsphere.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryEventListener {
    private final InventoryService inventoryService;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String IDEMPOTENCY_KEY_PREFIX = "inventory:event:processed:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    /**
     * Listen to order.placed event and auto-reserve inventory
     */
    @KafkaListener(
            topics = "order.placed",
            groupId = "inventory-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleOrderPlaced(String message, Acknowledgment ack) {
         log.info("Received order.placed event in inventory-service: {}", message);
         try {
             EventEnvelope<OrderPlacedPayload> envelope = objectMapper.readValue(
                     message,
                     new TypeReference<EventEnvelope<OrderPlacedPayload>>() {}
             );

             String eventId = envelope.getId();
             String idempotencyKey = IDEMPOTENCY_KEY_PREFIX + eventId;

             // Idempotency check
             Boolean isNew = redisTemplate.opsForValue().setIfAbsent(idempotencyKey, "PROCESSED", IDEMPOTENCY_TTL);
             if (Boolean.FALSE.equals(isNew)) {
                 log.warn("Event {} already processed, skipping duplicate order.placed event.", eventId);
                 ack.acknowledge();
                 return;
             }

             OrderPlacedPayload payload = envelope.getPayload();
             if (payload == null) {
                 log.error("Event {} has null payload, ignoring.", eventId);
                 ack.acknowledge();
                 return;
             }

             String orderId = payload.getOrderNumber();
             String userId = payload.getUserId();
             String totalAmount = payload.getTotalAmount() != null ? payload.getTotalAmount().toString() : "0.0";
             String sessionId = payload.getSessionId();

             if (payload.getItems() == null || payload.getItems().isEmpty()) {
                 log.warn("No items found in order: {}", orderId);
                 ack.acknowledge();
                 return;
             }

             try {
                 // Reserve inventory for each item
                 for (OrderPlacedPayload.OrderItemPayload item : payload.getItems()) {
                     inventoryService.reserveInventoryForOrderWithContext(
                             item.getSku(),
                             item.getQuantity(),
                             orderId,
                             userId,
                             totalAmount,
                             sessionId
                     );
                 }
                 log.info("Successfully reserved inventory for all items in order: {}", orderId);
             } catch (Exception e) {
                 log.error("Failed to reserve inventory for order {}. Triggering compensation SAGA.", orderId, e);
                 
                 // Publish inventory.reservation.failed event for SAGA compensation
                 InventoryReservationFailedPayload failedPayload = InventoryReservationFailedPayload.builder()
                         .sessionId(sessionId)
                         .orderNumber(orderId)
                         .userId(userId)
                         .reason(e.getMessage() != null ? e.getMessage() : "Insufficient stock or item not found")
                         .build();

                 EventEnvelope<InventoryReservationFailedPayload> failedEnvelope = EventEnvelope.<InventoryReservationFailedPayload>builder()
                         .type("INVENTORY_RESERVATION_FAILED")
                         .source("inventory-service")
                         .payload(failedPayload)
                         .build();

                 String jsonPayload = objectMapper.writeValueAsString(failedEnvelope);

                 OutboxEvent outboxEvent = OutboxEvent.builder()
                         .topic("inventory.reservation.failed")
                         .key(orderId)
                         .payload(jsonPayload)
                         .build();

                 outboxRepository.save(outboxEvent);
                 log.info("Saved outbox event for inventory.reservation.failed: order {}", orderId);
             }
             ack.acknowledge();
         } catch (Exception e) {
             log.error("Error processing order.placed event: {}", message, e);
             // We don't acknowledge on severe system errors to allow Kafka retry
         }
    }

    /**
     * Listen to order.cancelled event and auto-release reserved inventory
     */
    @KafkaListener(
            topics = "order.cancelled",
            groupId = "inventory-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderCancelled(String message, Acknowledgment ack) {
        log.info("Received order.cancelled event: {}", message);
        try {
            // Releasing is idempotent by design in DB
            Map<String, Object> event = parseJsonMessage(message);
            String orderId = event.get("orderId").toString();
            
            inventoryService.releaseReservationByOrder(orderId);
            log.info("Inventory released for cancelled order: {}", orderId);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing order.cancelled event: {}", message, e);
            ack.acknowledge(); // Acknowledge to prevent blocking queue, best effort
        }
    }

    /**
     * Listen to checkout.failed event and auto-release reserved inventory
     */
    @KafkaListener(
            topics = "checkout.failed",
            groupId = "inventory-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCheckoutFailed(String message, Acknowledgment ack) {
        log.info("Received checkout.failed event: {}", message);
        try {
            Map<String, Object> event = parseJsonMessage(message);
            String orderId = event.get("orderId").toString();
            
            inventoryService.releaseReservationByOrder(orderId);
            log.info("Inventory released for failed checkout: {}", orderId);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing checkout.failed event: {}", message, e);
            ack.acknowledge();
        }
    }

    /**
     * Listen to payment.failed event and auto-release reserved inventory
     */
    @KafkaListener(
            topics = "payment.failed",
            groupId = "inventory-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentFailed(String message, Acknowledgment ack) {
        log.info("Received payment.failed event: {}", message);
        try {
            Map<String, Object> event = parseJsonMessage(message);
            String orderId = event.get("orderId").toString();
            
            inventoryService.releaseReservationByOrder(orderId);
            log.info("Inventory released for failed payment: {}", orderId);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing payment.failed event: {}", message, e);
            ack.acknowledge();
        }
    }

    private Map<String, Object> parseJsonMessage(String message) {
        try {
            return objectMapper.readValue(message, java.util.Map.class);
        } catch (Exception e) {
            log.error("Failed to parse message: {}", message, e);
            throw new RuntimeException("Failed to parse Kafka message", e);
        }
    }
}
