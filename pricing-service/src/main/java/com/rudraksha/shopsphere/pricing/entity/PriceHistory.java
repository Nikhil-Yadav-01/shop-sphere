package com.rudraksha.shopsphere.pricing.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String productId;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal oldPrice;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal newPrice;
    
    private String changedBy;
    private String changeReason;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime changedAt;
    
    @PrePersist
    protected void onCreate() {
        changedAt = LocalDateTime.now();
    }
}
