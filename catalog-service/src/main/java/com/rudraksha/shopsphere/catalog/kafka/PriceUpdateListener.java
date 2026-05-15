package com.rudraksha.shopsphere.catalog.kafka;

import com.rudraksha.shopsphere.catalog.entity.Product;
import com.rudraksha.shopsphere.catalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceUpdateListener {

    private final ProductRepository productRepository;

    @KafkaListener(topics = "price.updated", groupId = "catalog-service-group")
    public void onPriceUpdated(Map<String, Object> event) {
        String productId = (String) event.get("productId");
        BigDecimal newPrice = new BigDecimal(event.get("newPrice").toString());
        BigDecimal originalPrice = event.get("originalPrice") != null ? new BigDecimal(event.get("originalPrice").toString()) : null;
        String currency = (String) event.get("currency");

        log.info("Received price update for product: {}. New price: {}", productId, newPrice);

        productRepository.findById(productId).ifPresentOrElse(product -> {
            product.setPrice(newPrice);
            if (originalPrice != null) product.setOriginalPrice(originalPrice);
            if (currency != null) product.setCurrency(currency);
            productRepository.save(product);
            log.info("Updated price for product: {}", productId);
        }, () -> log.warn("Product not found for price update: {}", productId));
    }
}
