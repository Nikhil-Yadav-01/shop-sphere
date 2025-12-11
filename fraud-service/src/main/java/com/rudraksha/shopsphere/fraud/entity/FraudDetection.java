package com.rudraksha.shopsphere.fraud.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_detection")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudDetection {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String transactionId;
    
    @Column(nullable = false)
    private Long orderId;
    
    @Column(nullable = false)
    private Long customerId;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    @Column(nullable = false)
    private String currency;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;
    
    @Column(nullable = false)
    private BigDecimal riskScore;
    
    @Column(nullable = false)
    private Boolean isFraudulent;
    
    @Column(columnDefinition = "TEXT")
    private String fraudReason;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FraudStatus status;
    
    @Column
    private String paymentMethod;
    
    @Column
    private String ipAddress;
    
    @Column
    private String deviceId;
    
    @Column(nullable = false)
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
    
    public enum TransactionType {
        PURCHASE, REFUND, CHARGEBACK
    }
    
    public enum FraudStatus {
        PENDING, ANALYZING, APPROVED, REJECTED, ESCALATED
    }
}
