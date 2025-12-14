package com.rudraksha.shopsphere.search.kafka;

import com.rudraksha.shopsphere.search.entity.SearchIndex;
import com.rudraksha.shopsphere.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductEventConsumer {

    private final SearchService searchService;

    @KafkaListener(topics = "product-created", groupId = "search-service-group")
    public void handleProductCreated(ProductEvent event) {
        log.info("Received product created event: {}", event.getProductId());
        try {
            SearchIndex searchIndex = buildSearchIndex(event);
            searchService.indexProduct(searchIndex);
            log.info("Product indexed successfully: {}", event.getProductId());
        } catch (Exception e) {
            log.error("Error indexing product: {}", event.getProductId(), e);
        }
    }

    @KafkaListener(topics = "product-updated", groupId = "search-service-group")
    public void handleProductUpdated(ProductEvent event) {
        log.info("Received product updated event: {}", event.getProductId());
        try {
            SearchIndex searchIndex = buildSearchIndex(event);
            searchService.updateIndexByProductId(event.getProductId(), searchIndex);
            log.info("Product index updated successfully: {}", event.getProductId());
        } catch (Exception e) {
            log.error("Error updating product index: {}", event.getProductId(), e);
        }
    }

    @KafkaListener(topics = "product-deleted", groupId = "search-service-group")
    public void handleProductDeleted(ProductEvent event) {
        log.info("Received product deleted event: {}", event.getProductId());
        try {
            searchService.deleteIndexByProductId(event.getProductId());
            log.info("Product index deleted successfully: {}", event.getProductId());
        } catch (Exception e) {
            log.error("Error deleting product index: {}", event.getProductId(), e);
        }
    }

    private SearchIndex buildSearchIndex(ProductEvent event) {
        return SearchIndex.builder()
                .productId(event.getProductId())
                .name(event.getProductName())
                .description(event.getDescription())
                .sku(event.getSku())
                .price(event.getPrice())
                .categoryId(event.getCategoryId())
                .categoryName(event.getCategoryName())
                .tags(event.getTags())
                .status(event.getStatus())
                .rating(event.getRating())
                .reviewCount(event.getReviewCount())
                .inStock(event.getInStock())
                .brand(event.getBrand())
                .build();
    }
}
