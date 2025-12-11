package com.rudraksha.shopsphere.fraud.service;

import com.rudraksha.shopsphere.fraud.dto.FraudCheckRequest;
import com.rudraksha.shopsphere.fraud.dto.FraudCheckResponse;
import com.rudraksha.shopsphere.fraud.entity.FraudDetection;
import com.rudraksha.shopsphere.fraud.repository.FraudDetectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {
    
    private final FraudDetectionRepository fraudDetectionRepository;
    private final FraudRuleService fraudRuleService;
    private final FraudEventPublisher fraudEventPublisher;
    
    public FraudCheckResponse checkFraud(FraudCheckRequest request) {
        log.info("Starting fraud check for transaction: {}", request.getTransactionId());
        
        // Check if transaction already exists
        var existingFraud = fraudDetectionRepository.findByTransactionId(request.getTransactionId());
        if (existingFraud.isPresent()) {
            log.warn("Transaction already analyzed: {}", request.getTransactionId());
            return mapToResponse(existingFraud.get());
        }
        
        // Calculate risk score
        BigDecimal riskScore = calculateRiskScore(request);
        
        // Determine if fraudulent
        boolean isFraudulent = riskScore.compareTo(new BigDecimal("70")) >= 0;
        
        // Create fraud detection record
        FraudDetection fraudDetection = FraudDetection.builder()
                .transactionId(request.getTransactionId())
                .orderId(request.getOrderId())
                .customerId(request.getCustomerId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .transactionType(FraudDetection.TransactionType.valueOf(
                        request.getTransactionType() != null ? request.getTransactionType() : "PURCHASE"))
                .riskScore(riskScore)
                .isFraudulent(isFraudulent)
                .fraudReason(isFraudulent ? "High risk score detected" : null)
                .status(isFraudulent ? FraudDetection.FraudStatus.REJECTED : FraudDetection.FraudStatus.APPROVED)
                .paymentMethod(request.getPaymentMethod())
                .ipAddress(request.getIpAddress())
                .deviceId(request.getDeviceId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        var saved = fraudDetectionRepository.save(fraudDetection);
        FraudCheckResponse response = mapToResponse(saved);
        
        // Publish events to Kafka
        fraudEventPublisher.publishFraudDetectionEvent(response);
        if (isFraudulent) {
            fraudEventPublisher.publishFraudAlert(response);
        } else {
            fraudEventPublisher.publishTransactionApprovedEvent(response);
        }
        
        log.info("Fraud check completed for transaction: {} - Risk Score: {}, Fraudulent: {}", 
                request.getTransactionId(), riskScore, isFraudulent);
        
        return response;
    }
    
    private BigDecimal calculateRiskScore(FraudCheckRequest request) {
        BigDecimal riskScore = BigDecimal.ZERO;
        
        // Rule 1: High amount threshold
        if (request.getAmount().compareTo(new BigDecimal("10000")) > 0) {
            riskScore = riskScore.add(new BigDecimal("30"));
        } else if (request.getAmount().compareTo(new BigDecimal("5000")) > 0) {
            riskScore = riskScore.add(new BigDecimal("15"));
        }
        
        // Rule 2: Card not present
        if ("CARD_NOT_PRESENT".equalsIgnoreCase(request.getPaymentMethod())) {
            riskScore = riskScore.add(new BigDecimal("25"));
        }
        
        // Rule 3: Missing device ID (high risk)
        if (request.getDeviceId() == null || request.getDeviceId().isEmpty()) {
            riskScore = riskScore.add(new BigDecimal("15"));
        }
        
        // Rule 4: Velocity check - check customer history
        List<FraudDetection> customerTransactions = fraudDetectionRepository
                .findByCustomerId(request.getCustomerId());
        if (customerTransactions.size() > 10) {
            riskScore = riskScore.add(new BigDecimal("10"));
        }
        
        // Cap risk score at 100
        if (riskScore.compareTo(new BigDecimal("100")) > 0) {
            riskScore = new BigDecimal("100");
        }
        
        return riskScore;
    }
    
    public FraudCheckResponse getFraudDetectionById(Long id) {
        return fraudDetectionRepository.findById(id)
                .map(this::mapToResponse)
                .orElse(null);
    }
    
    public FraudCheckResponse getFraudDetectionByTransactionId(String transactionId) {
        return fraudDetectionRepository.findByTransactionId(transactionId)
                .map(this::mapToResponse)
                .orElse(null);
    }
    
    public List<FraudDetection> getCustomerFraudHistory(Long customerId) {
        return fraudDetectionRepository.findFraudulentTransactionsByCustomer(customerId);
    }
    
    private FraudCheckResponse mapToResponse(FraudDetection fraudDetection) {
        return FraudCheckResponse.builder()
                .id(fraudDetection.getId())
                .transactionId(fraudDetection.getTransactionId())
                .orderId(fraudDetection.getOrderId())
                .customerId(fraudDetection.getCustomerId())
                .riskScore(fraudDetection.getRiskScore())
                .isFraudulent(fraudDetection.getIsFraudulent())
                .status(fraudDetection.getStatus().toString())
                .fraudReason(fraudDetection.getFraudReason())
                .createdAt(fraudDetection.getCreatedAt())
                .updatedAt(fraudDetection.getUpdatedAt())
                .message(fraudDetection.getIsFraudulent() ? "Transaction rejected due to fraud detection" : 
                        "Transaction approved")
                .build();
    }
}
