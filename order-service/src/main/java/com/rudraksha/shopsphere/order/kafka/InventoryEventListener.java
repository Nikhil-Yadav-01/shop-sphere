package com.rudraksha.shopsphere.order.kafka;

import com.rudraksha.shopsphere.order.entity.Order;
import com.rudraksha.shopsphere.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventListener {

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "inventory.reserved", groupId = "order-service-group")
    public void onInventoryReserved(String message) {
        log.info("Received inventory.reserved event: {}", message);
        try {
            JsonNode node = objectMapper.readTree(message);
            if (node.has("orderNumber")) {
                String orderNumber = node.get("orderNumber").asText();
                orderRepository.findByOrderNumber(orderNumber).ifPresent(order -> {
                    if (order.getStatus() == Order.OrderStatus.PENDING) {
                        order.setStatus(Order.OrderStatus.PROCESSING);
                        orderRepository.save(order);
                        log.info("Order {} status updated to PROCESSING after inventory reservation.", orderNumber);
                    }
                });
            }
        } catch (Exception e) {
            log.error("Error processing inventory.reserved event", e);
        }
    }
}
