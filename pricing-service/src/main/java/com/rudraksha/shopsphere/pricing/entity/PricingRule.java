package com.rudraksha.shopsphere.pricing.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pricing_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String ruleName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleType ruleType;
    
    @Column(precision = 5, scale = 2)
    private BigDecimal discountPercentage;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal discountFixed;
    
    private Integer minQuantity;
    private Integer maxQuantity;
    
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum RuleType {
        BULK_DISCOUNT, SEASONAL, TIER_BASED, CATEGORY, LOCATION
    }
    
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return isActive && 
               (validFrom == null || now.isAfter(validFrom)) && 
               (validUntil == null || now.isBefore(validUntil));
    }
}
