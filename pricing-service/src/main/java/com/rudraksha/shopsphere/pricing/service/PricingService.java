package com.rudraksha.shopsphere.pricing.service;

import com.rudraksha.shopsphere.pricing.dto.request.*;
import com.rudraksha.shopsphere.pricing.dto.response.*;
import java.util.List;

public interface PricingService {
    // Product Price Operations
    ProductPriceResponse createProductPrice(CreateProductPriceRequest request);
    ProductPriceResponse getProductPrice(String productId);
    List<ProductPriceResponse> getAllActivePrices();
    ProductPriceResponse updateProductPrice(String productId, CreateProductPriceRequest request);
    void deleteProductPrice(String productId);
    
    // Price Calculation
    PriceCalculationResponse calculatePrice(CalculatePriceRequest request);
    
    // Pricing Rules
    PricingRuleResponse createPricingRule(CreatePricingRuleRequest request);
    PricingRuleResponse getPricingRule(Long ruleId);
    List<PricingRuleResponse> getAllPricingRules();
    List<PricingRuleResponse> getActiveRules();
    PricingRuleResponse updatePricingRule(Long ruleId, CreatePricingRuleRequest request);
    void deletePricingRule(Long ruleId);
    
    // Pricing Tiers
    List<PricingTierResponse> getAllPricingTiers();
    PricingTierResponse getPricingTier(Long tierId);
    PricingTierResponse getTierForQuantity(Integer quantity);
}
