package com.rudraksha.shopsphere.pricing.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendPriceUpdatedEvent(String productId, BigDecimal newPrice, BigDecimal oldPrice) {
        Map<String, Object> event = new HashMap<>();
        event.put("productId", productId);
        event.put("newPrice", newPrice);
        event.put("oldPrice", oldPrice);
        event.put("timestamp", System.currentTimeMillis());

        log.info("Publishing price.updated event for product: {}", productId);
        kafkaTemplate.send("price.updated", productId, event);
    }
}
