package com.rudraksha.shopsphere.catalog.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class TestController {

    @GetMapping("/test")
    public Map<String, String> test() {
        return Map.of(
            "service", "catalog-service",
            "status", "running",
            "message", "Gateway integration test successful"
        );
    }

    @GetMapping("/products")
    public Map<String, Object> getProducts() {
        return Map.of(
            "products", java.util.List.of(
                Map.of("id", "1", "name", "Test Product 1", "sku", "TEST-001"),
                Map.of("id", "2", "name", "Test Product 2", "sku", "TEST-002")
            ),
            "total", 2,
            "message", "Mock data for gateway testing"
        );
    }
}