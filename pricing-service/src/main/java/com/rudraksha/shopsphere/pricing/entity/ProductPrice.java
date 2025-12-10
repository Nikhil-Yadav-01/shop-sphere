package com.rudraksha.shopsphere.pricing.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_prices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String productId;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;
    
    @Column(nullable = false)
    @Builder.Default
    private String currency = "USD";
    
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
}
