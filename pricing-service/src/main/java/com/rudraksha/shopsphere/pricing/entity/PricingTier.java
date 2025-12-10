package com.rudraksha.shopsphere.pricing.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pricing_tiers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingTier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String tierName;
    
    @Column(nullable = false)
    private Integer minQuantity;
    
    private Integer maxQuantity;
    
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercentage;
    
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
    
    public boolean appliesToQuantity(int quantity) {
        if (quantity < minQuantity) return false;
        return maxQuantity == null || quantity <= maxQuantity;
    }
}
