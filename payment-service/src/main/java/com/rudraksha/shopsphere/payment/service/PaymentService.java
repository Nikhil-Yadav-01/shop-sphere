package com.rudraksha.shopsphere.payment.service;

import com.rudraksha.shopsphere.payment.dto.request.ProcessPaymentRequest;
import com.rudraksha.shopsphere.payment.dto.request.RefundPaymentRequest;
import com.rudraksha.shopsphere.payment.dto.response.PaymentResponse;
import com.rudraksha.shopsphere.payment.entity.Payment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface PaymentService {
    PaymentResponse processPayment(ProcessPaymentRequest request);
    PaymentResponse getPaymentById(Long id);
    PaymentResponse getPaymentByTransactionId(String transactionId);
    Page<PaymentResponse> getPaymentsByOrderNumber(String orderNumber, Pageable pageable);
    Page<PaymentResponse> getPaymentsByUserId(String userId, Pageable pageable);
    Page<PaymentResponse> getPaymentsByStatus(Payment.PaymentStatus status, Pageable pageable);
    PaymentResponse refundPayment(RefundPaymentRequest request);
    PaymentResponse updatePaymentStatus(Long id, Payment.PaymentStatus status);
    Page<PaymentResponse> getAllPayments(Pageable pageable);
}
