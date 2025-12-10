package com.rudraksha.shopsphere.payment.service;

import com.rudraksha.shopsphere.payment.dto.request.PaymentRequest;
import com.rudraksha.shopsphere.payment.dto.response.PaymentResponse;

import java.util.List;

public interface PaymentService {
    PaymentResponse processPayment(PaymentRequest request);
    PaymentResponse getPayment(String paymentId);
    List<PaymentResponse> getPaymentsByOrder(String orderId);
    List<PaymentResponse> getPaymentsByUser(String userId);
    PaymentResponse refundPayment(String paymentId);
}