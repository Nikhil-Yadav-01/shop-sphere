package com.rudraksha.shopsphere.checkout.service.payment;

import com.rudraksha.shopsphere.checkout.dto.request.PaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "payment.gateway.provider", havingValue = "mock", matchIfMissing = true)
@Slf4j
public class MockPaymentService implements PaymentService {

    @Override
    public PaymentResult processPayment(PaymentRequest paymentRequest, BigDecimal amount, String orderNumber) {
        log.info("Processing mock payment for order: {} with amount: {}", orderNumber, amount);
        
        // Simulate payment processing delay
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Mock success/failure based on amount (fail if amount > 1000 for testing)
        boolean success = amount.compareTo(BigDecimal.valueOf(1000)) <= 0;
        String transactionId = "MOCK_TXN_" + UUID.randomUUID().toString().substring(0, 8);
        String message = success ? "Payment processed successfully" : "Payment failed - amount too high";
        String gatewayResponse = String.format("{\"status\":\"%s\",\"transaction_id\":\"%s\",\"amount\":%s}", 
                success ? "success" : "failed", transactionId, amount);

        log.info("Mock payment result for order {}: success={}, transactionId={}", 
                orderNumber, success, transactionId);

        return new PaymentResult(success, transactionId, message, gatewayResponse);
    }
}