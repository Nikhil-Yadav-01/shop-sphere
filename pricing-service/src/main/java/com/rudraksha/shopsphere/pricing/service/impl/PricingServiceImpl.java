package com.rudraksha.shopsphere.pricing.service.impl;

import com.rudraksha.shopsphere.pricing.dto.request.*;
import com.rudraksha.shopsphere.pricing.dto.response.*;
import com.rudraksha.shopsphere.pricing.entity.*;
import com.rudraksha.shopsphere.pricing.exception.PricingException;
import com.rudraksha.shopsphere.pricing.repository.*;
import com.rudraksha.shopsphere.pricing.service.PricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PricingServiceImpl implements PricingService {
    
    private final ProductPriceRepository productPriceRepository;
    private final PricingRuleRepository pricingRuleRepository;
    private final PricingTierRepository pricingTierRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    
    @Override
    public ProductPriceResponse createProductPrice(CreateProductPriceRequest request) {
        if (productPriceRepository.findByProductId(request.getProductId()).isPresent()) {
            throw new PricingException("Product with ID " + request.getProductId() + " already exists");
        }
        
        ProductPrice price = ProductPrice.builder()
                .productId(request.getProductId())
                .basePrice(request.getBasePrice())
                .currency(request.getCurrency())
                .build();
        
        price = productPriceRepository.save(price);
        log.info("Created product price for product: {}", request.getProductId());
        return mapToResponse(price);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ProductPriceResponse getProductPrice(String productId) {
        ProductPrice price = productPriceRepository.findByProductIdAndActiveTrue(productId)
                .orElseThrow(() -> new PricingException("Product not found: " + productId));
        return mapToResponse(price);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProductPriceResponse> getAllActivePrices() {
        return productPriceRepository.findAllByActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public ProductPriceResponse updateProductPrice(String productId, CreateProductPriceRequest request) {
        ProductPrice price = productPriceRepository.findByProductId(productId)
                .orElseThrow(() -> new PricingException("Product not found: " + productId));
        
        BigDecimal oldPrice = price.getBasePrice();
        price.setBasePrice(request.getBasePrice());
        price.setCurrency(request.getCurrency());
        price = productPriceRepository.save(price);
        
        // Record price history
        PriceHistory history = PriceHistory.builder()
                .productId(productId)
                .oldPrice(oldPrice)
                .newPrice(request.getBasePrice())
                .changeReason("Manual update")
                .build();
        priceHistoryRepository.save(history);
        
        log.info("Updated product price for product: {} from {} to {}", 
                productId, oldPrice, request.getBasePrice());
        return mapToResponse(price);
    }
    
    @Override
    public void deleteProductPrice(String productId) {
        ProductPrice price = productPriceRepository.findByProductId(productId)
                .orElseThrow(() -> new PricingException("Product not found: " + productId));
        price.setActive(false);
        productPriceRepository.save(price);
        log.info("Deleted product price for product: {}", productId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PriceCalculationResponse calculatePrice(CalculatePriceRequest request) {
        ProductPrice product = productPriceRepository.findByProductIdAndActiveTrue(request.getProductId())
                .orElseThrow(() -> new PricingException("Product not found: " + request.getProductId()));
        
        BigDecimal basePrice = product.getBasePrice();
        BigDecimal unitPrice = basePrice;
        BigDecimal discountAmount = BigDecimal.ZERO;
        String appliedTier = "No tier";
        
        // Apply tier-based discount
        var tier = pricingTierRepository.findTierByQuantity(request.getQuantity());
        if (tier.isPresent()) {
            PricingTier pricingTier = tier.get();
            BigDecimal tierDiscount = basePrice.multiply(pricingTier.getDiscountPercentage())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            unitPrice = basePrice.subtract(tierDiscount);
            discountAmount = tierDiscount;
            appliedTier = pricingTier.getTierName();
        }
        
        // Apply rules
        List<PricingRule> activeRules = pricingRuleRepository.findAllActiveRules(LocalDateTime.now());
        String appliedRules = "None";
        BigDecimal ruleDiscount = BigDecimal.ZERO;
        
        for (PricingRule rule : activeRules) {
            if (rule.getMinQuantity() != null && request.getQuantity() < rule.getMinQuantity()) {
                continue;
            }
            if (rule.getMaxQuantity() != null && request.getQuantity() > rule.getMaxQuantity()) {
                continue;
            }
            
            if (rule.getDiscountPercentage() != null) {
                ruleDiscount = unitPrice.multiply(rule.getDiscountPercentage())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            } else if (rule.getDiscountFixed() != null) {
                ruleDiscount = rule.getDiscountFixed();
            }
            
            unitPrice = unitPrice.subtract(ruleDiscount);
            appliedRules = rule.getRuleName();
            break;
        }
        
        // Apply additional discount if provided
        if (request.getAdditionalDiscount() != null) {
            unitPrice = unitPrice.subtract(request.getAdditionalDiscount());
        }
        
        // Ensure unit price doesn't go below zero
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            unitPrice = BigDecimal.ZERO;
        }
        
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));
        totalPrice = totalPrice.setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal totalDiscount = basePrice.multiply(BigDecimal.valueOf(request.getQuantity()))
                .subtract(totalPrice);
        BigDecimal discountPercentage = basePrice.compareTo(BigDecimal.ZERO) > 0 ? 
                totalDiscount.multiply(BigDecimal.valueOf(100))
                        .divide(basePrice.multiply(BigDecimal.valueOf(request.getQuantity())), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        
        return PriceCalculationResponse.builder()
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .basePrice(basePrice)
                .unitPrice(unitPrice)
                .totalPrice(totalPrice)
                .discountAmount(totalDiscount)
                .discountPercentage(discountPercentage)
                .appliedRules(appliedRules)
                .appliedTier(appliedTier)
                .build();
    }
    
    @Override
    public PricingRuleResponse createPricingRule(CreatePricingRuleRequest request) {
        PricingRule rule = PricingRule.builder()
                .ruleName(request.getRuleName())
                .ruleType(request.getRuleType())
                .discountPercentage(request.getDiscountPercentage())
                .discountFixed(request.getDiscountFixed())
                .minQuantity(request.getMinQuantity())
                .maxQuantity(request.getMaxQuantity())
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .build();
        
        rule = pricingRuleRepository.save(rule);
        log.info("Created pricing rule: {}", request.getRuleName());
        return mapToRuleResponse(rule);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PricingRuleResponse getPricingRule(Long ruleId) {
        PricingRule rule = pricingRuleRepository.findById(ruleId)
                .orElseThrow(() -> new PricingException("Pricing rule not found: " + ruleId));
        return mapToRuleResponse(rule);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PricingRuleResponse> getAllPricingRules() {
        return pricingRuleRepository.findAll()
                .stream()
                .map(this::mapToRuleResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PricingRuleResponse> getActiveRules() {
        return pricingRuleRepository.findAllActiveRules(LocalDateTime.now())
                .stream()
                .map(this::mapToRuleResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public PricingRuleResponse updatePricingRule(Long ruleId, CreatePricingRuleRequest request) {
        PricingRule rule = pricingRuleRepository.findById(ruleId)
                .orElseThrow(() -> new PricingException("Pricing rule not found: " + ruleId));
        
        rule.setRuleName(request.getRuleName());
        rule.setRuleType(request.getRuleType());
        rule.setDiscountPercentage(request.getDiscountPercentage());
        rule.setDiscountFixed(request.getDiscountFixed());
        rule.setMinQuantity(request.getMinQuantity());
        rule.setMaxQuantity(request.getMaxQuantity());
        rule.setValidFrom(request.getValidFrom());
        rule.setValidUntil(request.getValidUntil());
        
        rule = pricingRuleRepository.save(rule);
        log.info("Updated pricing rule: {}", ruleId);
        return mapToRuleResponse(rule);
    }
    
    @Override
    public void deletePricingRule(Long ruleId) {
        PricingRule rule = pricingRuleRepository.findById(ruleId)
                .orElseThrow(() -> new PricingException("Pricing rule not found: " + ruleId));
        rule.setIsActive(false);
        pricingRuleRepository.save(rule);
        log.info("Deleted pricing rule: {}", ruleId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PricingTierResponse> getAllPricingTiers() {
        return pricingTierRepository.findAllOrderByQuantity()
                .stream()
                .map(this::mapToTierResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public PricingTierResponse getPricingTier(Long tierId) {
        PricingTier tier = pricingTierRepository.findById(tierId)
                .orElseThrow(() -> new PricingException("Pricing tier not found: " + tierId));
        return mapToTierResponse(tier);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PricingTierResponse getTierForQuantity(Integer quantity) {
        return pricingTierRepository.findTierByQuantity(quantity)
                .map(this::mapToTierResponse)
                .orElseThrow(() -> new PricingException("No tier found for quantity: " + quantity));
    }
    
    private ProductPriceResponse mapToResponse(ProductPrice price) {
        return ProductPriceResponse.builder()
                .id(price.getId())
                .productId(price.getProductId())
                .basePrice(price.getBasePrice())
                .currency(price.getCurrency())
                .active(price.getActive())
                .createdAt(price.getCreatedAt())
                .updatedAt(price.getUpdatedAt())
                .build();
    }
    
    private PricingRuleResponse mapToRuleResponse(PricingRule rule) {
        return PricingRuleResponse.builder()
                .id(rule.getId())
                .ruleName(rule.getRuleName())
                .ruleType(rule.getRuleType())
                .discountPercentage(rule.getDiscountPercentage())
                .discountFixed(rule.getDiscountFixed())
                .minQuantity(rule.getMinQuantity())
                .maxQuantity(rule.getMaxQuantity())
                .validFrom(rule.getValidFrom())
                .validUntil(rule.getValidUntil())
                .isActive(rule.getIsActive())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
    
    private PricingTierResponse mapToTierResponse(PricingTier tier) {
        return PricingTierResponse.builder()
                .id(tier.getId())
                .tierName(tier.getTierName())
                .minQuantity(tier.getMinQuantity())
                .maxQuantity(tier.getMaxQuantity())
                .discountPercentage(tier.getDiscountPercentage())
                .createdAt(tier.getCreatedAt())
                .updatedAt(tier.getUpdatedAt())
                .build();
    }
}
