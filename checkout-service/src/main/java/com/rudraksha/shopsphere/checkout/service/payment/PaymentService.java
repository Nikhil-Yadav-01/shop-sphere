package com.rudraksha.shopsphere.checkout.service.payment;

import com.rudraksha.shopsphere.checkout.dto.request.PaymentRequest;

import java.math.BigDecimal;

public interface PaymentService {
    PaymentResult processPayment(PaymentRequest paymentRequest, BigDecimal amount, String orderNumber);
    
    record PaymentResult(
            boolean success,
            String transactionId,
            String message,
            String gatewayResponse
    ) {}
}