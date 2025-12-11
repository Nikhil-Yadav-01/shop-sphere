package com.rudraksha.shopsphere.fraud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudCheckResponse {
    
    private Long id;
    
    private String transactionId;
    
    private Long orderId;
    
    private Long customerId;
    
    private BigDecimal riskScore;
    
    private Boolean isFraudulent;
    
    private String status;
    
    private String fraudReason;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private String message;
}
