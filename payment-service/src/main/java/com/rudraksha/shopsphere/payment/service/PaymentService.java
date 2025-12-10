package com.rudraksha.shopsphere.payment.service;

import com.rudraksha.shopsphere.payment.dto.request.ProcessPaymentRequest;
import com.rudraksha.shopsphere.payment.dto.request.RefundPaymentRequest;
import com.rudraksha.shopsphere.payment.dto.response.PaymentResponse;
import com.rudraksha.shopsphere.payment.entity.Payment;

import java.util.List;

public interface PaymentService {
    PaymentResponse processPayment(ProcessPaymentRequest request);
    PaymentResponse getPaymentById(Long id);
    PaymentResponse getPaymentByTransactionId(String transactionId);
    List<PaymentResponse> getPaymentsByOrderId(Long orderId);
    List<PaymentResponse> getPaymentsByCustomerId(Long customerId);
    List<PaymentResponse> getPaymentsByStatus(Payment.PaymentStatus status);
    PaymentResponse refundPayment(RefundPaymentRequest request);
    PaymentResponse updatePaymentStatus(Long id, Payment.PaymentStatus status);
    List<PaymentResponse> getAllPayments();
}
