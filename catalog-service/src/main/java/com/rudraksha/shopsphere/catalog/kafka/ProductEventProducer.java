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
        Map<String, Object> event = createEvent("PRODUCT_CREATED", product);
        
        kafkaTemplate.send("product.created", product.getId(), event);
        kafkaTemplate.send("search.product.index", product.getId(), event);
        log.info("Published product created and indexing events for product ID: {}", product.getId());
    }

    public void publishProductUpdated(Product product) {
        Map<String, Object> event = createEvent("PRODUCT_UPDATED", product);
        
        kafkaTemplate.send("product.updated", product.getId(), event);
        kafkaTemplate.send("search.product.index", product.getId(), event);
        log.info("Published product updated and indexing events for product ID: {}", product.getId());
    }

    public void publishProductDeleted(String productId) {
        kafkaTemplate.send("product.deleted", productId, "deleted");
        kafkaTemplate.send("search.product.delete", productId, "deleted");
        log.info("Published product deletion and de-indexing events for product ID: {}", productId);
    }

    private Map<String, Object> createEvent(String type, Product product) {
        return Map.of(
            "eventType", type,
            "productId", product.getId(),
            "sku", product.getSku(),
            "name", product.getName(),
            "description", product.getDescription() != null ? product.getDescription() : "",
            "price", product.getPrice() != null ? product.getPrice() : 0,
            "categoryId", product.getCategoryId(),
            "status", product.getStatus().name(),
            "timestamp", java.time.LocalDateTime.now().toString()
        );
    }
}
