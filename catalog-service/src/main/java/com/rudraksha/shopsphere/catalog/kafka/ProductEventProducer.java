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

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publishProductCreated(Product product) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "PRODUCT_CREATED");
        event.put("productId", product.getId());
        event.put("sku", product.getSku());
        event.put("name", product.getName());
        event.put("price", product.getPrice());
        event.put("categoryId", product.getCategoryId());
        event.put("status", product.getStatus().name());
        
        kafkaTemplate.send("product-events", product.getId(), event.toString());
        log.info("Published product created event for product ID: {}", product.getId());
    }

    public void publishProductUpdated(Product product) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "PRODUCT_UPDATED");
        event.put("productId", product.getId());
        event.put("sku", product.getSku());
        event.put("name", product.getName());
        event.put("price", product.getPrice());
        event.put("categoryId", product.getCategoryId());
        event.put("status", product.getStatus().name());
        
        kafkaTemplate.send("product-events", product.getId(), event.toString());
        log.info("Published product updated event for product ID: {}", product.getId());
    }
}
