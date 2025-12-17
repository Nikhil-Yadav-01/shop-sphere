package com.rudraksha.shopsphere.catalog.kafka;

import com.rudraksha.shopsphere.catalog.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishProductCreated(Product product) {
        Map<String, Object> event = Map.of(
            "eventType", "PRODUCT_CREATED",
            "productId", product.getId(),
            "sku", product.getSku(),
            "name", product.getName(),
            "categoryId", product.getCategoryId(),
            "status", product.getStatus().name(),
            "timestamp", java.time.LocalDateTime.now().toString()
        );
        
        kafkaTemplate.send("product.created", product.getId(), event);
        log.info("Published product created event for product ID: {}", product.getId());
    }

    public void publishProductUpdated(Product product) {
        Map<String, Object> event = Map.of(
            "eventType", "PRODUCT_UPDATED",
            "productId", product.getId(),
            "sku", product.getSku(),
            "name", product.getName(),
            "categoryId", product.getCategoryId(),
            "status", product.getStatus().name(),
            "timestamp", java.time.LocalDateTime.now().toString()
        );
        
        kafkaTemplate.send("product.updated", product.getId(), event);
        log.info("Published product updated event for product ID: {}", product.getId());
    }
}
