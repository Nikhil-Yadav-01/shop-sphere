package com.rudraksha.shopsphere.payment.service.impl;

import com.rudraksha.shopsphere.payment.dto.request.ProcessPaymentRequest;
import com.rudraksha.shopsphere.payment.dto.request.RefundPaymentRequest;
import com.rudraksha.shopsphere.payment.dto.response.PaymentResponse;
import com.rudraksha.shopsphere.payment.entity.Payment;
import com.rudraksha.shopsphere.payment.exception.PaymentException;
import com.rudraksha.shopsphere.payment.repository.PaymentRepository;
import com.rudraksha.shopsphere.payment.service.PaymentService;
import lombok.AllArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public PaymentResponse processPayment(ProcessPaymentRequest request) {
        String transactionId = generateTransactionId();

        Payment payment = Payment.builder()
                .transactionId(transactionId)
                .orderId(request.getOrderId())
                .customerId(request.getCustomerId())
                .status(Payment.PaymentStatus.PROCESSING)
                .method(request.getMethod())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentGatewayId(UUID.randomUUID().toString())
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        
        // Simulate payment processing
        if (simulatePaymentGateway(request)) {
            savedPayment.setStatus(Payment.PaymentStatus.SUCCESS);
            savedPayment.setProcessedAt(LocalDateTime.now());
            publishPaymentEvent("PAYMENT_SUCCESS", transactionId, savedPayment.getOrderId());
        } else {
            savedPayment.setStatus(Payment.PaymentStatus.FAILED);
            savedPayment.setFailureReason("Payment gateway declined");
            publishPaymentEvent("PAYMENT_FAILED", transactionId, savedPayment.getOrderId());
        }

        Payment updatedPayment = paymentRepository.save(savedPayment);
        return mapToResponse(updatedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentException("Payment not found with id: " + id));
        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByTransactionId(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new PaymentException("Payment not found with transaction id: " + transactionId));
        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByCustomerId(Long customerId) {
        return paymentRepository.findByCustomerId(customerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByStatus(Payment.PaymentStatus status) {
        return paymentRepository.findByStatus(status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PaymentResponse refundPayment(RefundPaymentRequest request) {
        Payment payment = paymentRepository.findByTransactionId(request.getTransactionId())
                .orElseThrow(() -> new PaymentException("Payment not found with transaction id: " + request.getTransactionId()));

        if (payment.getStatus() != Payment.PaymentStatus.SUCCESS) {
            throw new PaymentException("Can only refund successful payments");
        }

        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        Payment refundedPayment = paymentRepository.save(payment);
        
        publishPaymentEvent("PAYMENT_REFUNDED", payment.getTransactionId(), payment.getOrderId());
        
        return mapToResponse(refundedPayment);
    }

    @Override
    public PaymentResponse updatePaymentStatus(Long id, Payment.PaymentStatus status) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentException("Payment not found with id: " + id));

        payment.setStatus(status);
        Payment updatedPayment = paymentRepository.save(payment);

        publishPaymentEvent("PAYMENT_STATUS_UPDATED", updatedPayment.getTransactionId(), updatedPayment.getOrderId());

        return mapToResponse(updatedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private String generateTransactionId() {
        return "TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private boolean simulatePaymentGateway(ProcessPaymentRequest request) {
        // Simulate payment processing - randomly succeed/fail for demo
        return Math.random() > 0.1; // 90% success rate
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .transactionId(payment.getTransactionId())
                .orderId(payment.getOrderId())
                .customerId(payment.getCustomerId())
                .status(payment.getStatus())
                .method(payment.getMethod())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentGatewayId(payment.getPaymentGatewayId())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .processedAt(payment.getProcessedAt())
                .build();
    }

    private void publishPaymentEvent(String eventType, String transactionId, Long orderId) {
        try {
            String message = eventType + ":" + transactionId + ":" + orderId + ":" + LocalDateTime.now();
            kafkaTemplate.send("payment-events", message);
        } catch (Exception e) {
            System.err.println("Failed to publish payment event: " + e.getMessage());
        }
    }
}
