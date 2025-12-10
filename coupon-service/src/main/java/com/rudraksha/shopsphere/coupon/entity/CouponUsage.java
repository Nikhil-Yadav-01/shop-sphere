package com.rudraksha.shopsphere.coupon.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_usage")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String couponCode;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private String orderId;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime usedAt;
    
    @PrePersist
    protected void onCreate() {
        usedAt = LocalDateTime.now();
    }
}