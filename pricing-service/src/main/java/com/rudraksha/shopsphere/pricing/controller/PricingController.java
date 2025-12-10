package com.rudraksha.shopsphere.pricing.controller;

import com.rudraksha.shopsphere.pricing.dto.request.*;
import com.rudraksha.shopsphere.pricing.dto.response.*;
import com.rudraksha.shopsphere.pricing.service.PricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pricing")
@RequiredArgsConstructor
public class PricingController {
    
    private final PricingService pricingService;
    
    // Product Price Endpoints
    @PostMapping("/prices")
    public ResponseEntity<ProductPriceResponse> createProductPrice(@Valid @RequestBody CreateProductPriceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pricingService.createProductPrice(request));
    }
    
    @GetMapping("/prices/{productId}")
    public ResponseEntity<ProductPriceResponse> getProductPrice(@PathVariable String productId) {
        return ResponseEntity.ok(pricingService.getProductPrice(productId));
    }
    
    @GetMapping("/prices")
    public ResponseEntity<List<ProductPriceResponse>> getAllActivePrices() {
        return ResponseEntity.ok(pricingService.getAllActivePrices());
    }
    
    @PutMapping("/prices/{productId}")
    public ResponseEntity<ProductPriceResponse> updateProductPrice(
            @PathVariable String productId,
            @Valid @RequestBody CreateProductPriceRequest request) {
        return ResponseEntity.ok(pricingService.updateProductPrice(productId, request));
    }
    
    @DeleteMapping("/prices/{productId}")
    public ResponseEntity<Void> deleteProductPrice(@PathVariable String productId) {
        pricingService.deleteProductPrice(productId);
        return ResponseEntity.noContent().build();
    }
    
    // Price Calculation Endpoint
    @PostMapping("/calculate")
    public ResponseEntity<PriceCalculationResponse> calculatePrice(@Valid @RequestBody CalculatePriceRequest request) {
        return ResponseEntity.ok(pricingService.calculatePrice(request));
    }
    
    // Pricing Rules Endpoints
    @PostMapping("/rules")
    public ResponseEntity<PricingRuleResponse> createPricingRule(@Valid @RequestBody CreatePricingRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pricingService.createPricingRule(request));
    }
    
    @GetMapping("/rules/{ruleId}")
    public ResponseEntity<PricingRuleResponse> getPricingRule(@PathVariable Long ruleId) {
        return ResponseEntity.ok(pricingService.getPricingRule(ruleId));
    }
    
    @GetMapping("/rules")
    public ResponseEntity<List<PricingRuleResponse>> getAllPricingRules() {
        return ResponseEntity.ok(pricingService.getAllPricingRules());
    }
    
    @GetMapping("/rules/active")
    public ResponseEntity<List<PricingRuleResponse>> getActiveRules() {
        return ResponseEntity.ok(pricingService.getActiveRules());
    }
    
    @PutMapping("/rules/{ruleId}")
    public ResponseEntity<PricingRuleResponse> updatePricingRule(
            @PathVariable Long ruleId,
            @Valid @RequestBody CreatePricingRuleRequest request) {
        return ResponseEntity.ok(pricingService.updatePricingRule(ruleId, request));
    }
    
    @DeleteMapping("/rules/{ruleId}")
    public ResponseEntity<Void> deletePricingRule(@PathVariable Long ruleId) {
        pricingService.deletePricingRule(ruleId);
        return ResponseEntity.noContent().build();
    }
    
    // Pricing Tiers Endpoints
    @GetMapping("/tiers")
    public ResponseEntity<List<PricingTierResponse>> getAllPricingTiers() {
        return ResponseEntity.ok(pricingService.getAllPricingTiers());
    }
    
    @GetMapping("/tiers/{tierId}")
    public ResponseEntity<PricingTierResponse> getPricingTier(@PathVariable Long tierId) {
        return ResponseEntity.ok(pricingService.getPricingTier(tierId));
    }
    
    @GetMapping("/tiers/quantity/{quantity}")
    public ResponseEntity<PricingTierResponse> getTierForQuantity(@PathVariable Integer quantity) {
        return ResponseEntity.ok(pricingService.getTierForQuantity(quantity));
    }
}
