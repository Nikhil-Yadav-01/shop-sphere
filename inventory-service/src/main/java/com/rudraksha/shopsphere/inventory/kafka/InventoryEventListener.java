package com.rudraksha.shopsphere.inventory.kafka;

import com.rudraksha.shopsphere.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryEventListener {
    private final InventoryService inventoryService;

    /**
     * Listen to order.placed event and auto-reserve inventory
     * Expected message format: {"orderId": 123, "items": [{"sku": "ABC", "quantity": 5}]}
     */
    @KafkaListener(topics = "order.placed", groupId = "inventory-service")
    public void handleOrderPlaced(String message) {
        log.info("Received order.placed event: {}", message);
        try {
            // Parse JSON message
            Map<String, Object> event = parseJsonMessage(message);
            String orderId = event.get("orderId").toString();
            
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> items = 
                (java.util.List<Map<String, Object>>) event.get("items");
            
            if (items == null || items.isEmpty()) {
                log.warn("No items found in order: {}", orderId);
                return;
            }
            
            // Reserve inventory for each item
            for (Map<String, Object> item : items) {
                String sku = item.get("sku").toString();
                Integer quantity = ((Number) item.get("quantity")).intValue();
                
                try {
                    inventoryService.reserveInventoryForOrder(sku, quantity, orderId);
                    log.info("Inventory reserved for order {}: {} x {}", orderId, sku, quantity);
                } catch (Exception e) {
                    log.error("Failed to reserve inventory for order {}: {} x {}", orderId, sku, quantity, e);
                    // Emit inventory.reservation.failed event for saga compensation
                    throw new RuntimeException("Inventory reservation failed for order: " + orderId, e);
                }
            }
        } catch (Exception e) {
            log.error("Error processing order.placed event: {}", message, e);
            throw new RuntimeException("Failed to process order.placed event", e);
        }
    }

    /**
     * Listen to order.cancelled event and auto-release reserved inventory
     */
    @KafkaListener(topics = "order.cancelled", groupId = "inventory-service")
    public void handleOrderCancelled(String message) {
        log.info("Received order.cancelled event: {}", message);
        try {
            Map<String, Object> event = parseJsonMessage(message);
            String orderId = event.get("orderId").toString();
            
            inventoryService.releaseReservationByOrder(orderId);
            log.info("Inventory released for cancelled order: {}", orderId);
        } catch (Exception e) {
            log.error("Error processing order.cancelled event: {}", message, e);
            // Don't throw - log and move on (best effort compensation)
        }
    }

    /**
     * Listen to checkout.failed event and auto-release reserved inventory
     */
    @KafkaListener(topics = "checkout.failed", groupId = "inventory-service")
    public void handleCheckoutFailed(String message) {
        log.info("Received checkout.failed event: {}", message);
        try {
            Map<String, Object> event = parseJsonMessage(message);
            String orderId = event.get("orderId").toString();
            
            inventoryService.releaseReservationByOrder(orderId);
            log.info("Inventory released for failed checkout: {}", orderId);
        } catch (Exception e) {
            log.error("Error processing checkout.failed event: {}", message, e);
        }
    }

    /**
     * Listen to payment.failed event and auto-release reserved inventory
     */
    @KafkaListener(topics = "payment.failed", groupId = "inventory-service")
    public void handlePaymentFailed(String message) {
        log.info("Received payment.failed event: {}", message);
        try {
            Map<String, Object> event = parseJsonMessage(message);
            String orderId = event.get("orderId").toString();
            
            inventoryService.releaseReservationByOrder(orderId);
            log.info("Inventory released for failed payment: {}", orderId);
        } catch (Exception e) {
            log.error("Error processing payment.failed event: {}", message, e);
        }
    }

    private Map<String, Object> parseJsonMessage(String message) {
        try {
            // Simple JSON parsing without external library
            return new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(message, java.util.Map.class);
        } catch (Exception e) {
            log.error("Failed to parse message: {}", message, e);
            throw new RuntimeException("Failed to parse Kafka message", e);
        }
    }
}
