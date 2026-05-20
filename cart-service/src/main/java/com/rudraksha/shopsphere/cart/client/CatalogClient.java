package com.rudraksha.shopsphere.cart.client;

import com.rudraksha.shopsphere.cart.dto.response.ProductResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "catalog-service", path = "/api/v1/products")
public interface CatalogClient {

    @GetMapping("/{id}")
    @CircuitBreaker(name = "catalog-service", fallbackMethod = "getProductByIdFallback")
    ProductResponse getProductById(@PathVariable("id") String id);

    @GetMapping("/batch")
    @CircuitBreaker(name = "catalog-service", fallbackMethod = "getProductsByIdsFallback")
    List<ProductResponse> getProductsByIds(@RequestParam("ids") List<String> ids);

    default ProductResponse getProductByIdFallback(String id, Throwable throwable) {
        LoggerFactory.getLogger(CatalogClient.class).error("Catalog Service fallback triggered for getProductById for ID: {} due to: {}", id, throwable.getMessage(), throwable);
        return ProductResponse.builder()
                .id(id)
                .name("Product temporarily unavailable")
                .price(BigDecimal.ZERO)
                .build();
    }

    default List<ProductResponse> getProductsByIdsFallback(List<String> ids, Throwable throwable) {
        LoggerFactory.getLogger(CatalogClient.class).error("Catalog Service fallback triggered for getProductsByIds for IDs: {} due to: {}", ids, throwable.getMessage(), throwable);
        return ids.stream()
                .map(id -> ProductResponse.builder()
                        .id(id)
                        .name("Product temporarily unavailable")
                        .price(BigDecimal.ZERO)
                        .build())
                .toList();
    }
}
