package com.rudraksha.shopsphere.cart.client;

import com.rudraksha.shopsphere.cart.dto.response.ProductResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

@FeignClient(name = "catalog-service", path = "/api/v1/products")
public interface CatalogClient {

    @GetMapping("/{id}")
    @CircuitBreaker(name = "catalog-service", fallbackMethod = "getProductByIdFallback")
    ProductResponse getProductById(@PathVariable("id") String id);

    default ProductResponse getProductByIdFallback(String id, Throwable throwable) {
        return ProductResponse.builder()
                .id(id)
                .name("Product temporarily unavailable")
                .price(BigDecimal.ZERO)
                .build();
    }
}
