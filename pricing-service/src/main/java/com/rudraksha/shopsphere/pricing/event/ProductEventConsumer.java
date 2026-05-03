package com.rudraksha.shopsphere.pricing.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class ProductEventConsumer {

    @KafkaListener(topics = "product.updated", groupId = "pricing-group")
    public void handleProductUpdated(Map<String, Object> event) {
        log.info("Received product.updated event: {}", event);
        // Logic to update local product metadata if needed would go here
        // For now, we just log it as per minimal requirement
        String productId = (String) event.get("id");
        log.info("Processing product update for id: {}", productId);
    }
}
