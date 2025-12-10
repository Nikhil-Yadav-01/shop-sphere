package com.rudraksha.shopsphere.coupon.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String code;
    
    @Column(nullable = false)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal minimumOrderAmount;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal maximumDiscountAmount;
    
    @Column(nullable = false)
    private Integer usageLimit;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer usedCount = 0;
    
    @Column(nullable = false)
    private LocalDateTime validFrom;
    
    @Column(nullable = false)
    private LocalDateTime validUntil;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    
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
    
    public enum DiscountType {
        PERCENTAGE, FIXED_AMOUNT
    }
    
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return active && 
               now.isAfter(validFrom) && 
               now.isBefore(validUntil) && 
               usedCount < usageLimit;
    }
}