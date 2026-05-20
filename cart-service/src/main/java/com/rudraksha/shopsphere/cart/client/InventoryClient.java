package com.rudraksha.shopsphere.cart.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.slf4j.LoggerFactory;

@FeignClient(name = "inventory-service", path = "/api/inventory")
public interface InventoryClient {

    @GetMapping("/check-availability")
    @CircuitBreaker(name = "inventory-service", fallbackMethod = "checkAvailabilityFallback")
    Boolean checkAvailability(@RequestParam("sku") String sku, @RequestParam("quantity") Integer quantity);

    default Boolean checkAvailabilityFallback(String sku, Integer quantity, Throwable throwable) {
        LoggerFactory.getLogger(InventoryClient.class).error("Inventory Service fallback triggered for checkAvailability for SKU: {}, quantity: {} due to: {}", sku, quantity, throwable.getMessage(), throwable);
        // Fallback to true to allow adding to cart even if inventory is down,
        // or false to be safe. Usually, for cart we might allow it but check again at checkout.
        // The report says "No Stock Validation in Cart" is a High issue.
        // So we should probably return true and log a warning, or return false if we want strictness.
        // Let's return true (optimistic) but log.
        return true;
    }
}
