package com.rudraksha.shopsphere.fraud.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String ruleName;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RuleType ruleType;
    
    @Column
    private BigDecimal threshold;
    
    @Column(nullable = false)
    private Boolean enabled;
    
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
    
    public enum RuleType {
        AMOUNT_THRESHOLD,
        VELOCITY_CHECK,
        GEOGRAPHIC_ANOMALY,
        DEVICE_MISMATCH,
        IP_BLACKLIST,
        CARD_NOT_PRESENT,
        HIGH_VALUE_TRANSACTION
    }
}
