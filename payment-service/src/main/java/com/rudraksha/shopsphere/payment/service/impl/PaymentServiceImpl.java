package com.rudraksha.shopsphere.payment.service.impl;

import com.rudraksha.shopsphere.payment.dto.request.ProcessPaymentRequest;
import com.rudraksha.shopsphere.payment.dto.request.RefundPaymentRequest;
import com.rudraksha.shopsphere.payment.dto.response.PaymentResponse;
import com.rudraksha.shopsphere.payment.entity.Payment;
import com.rudraksha.shopsphere.payment.exception.PaymentException;
import com.rudraksha.shopsphere.payment.repository.PaymentRepository;
import com.rudraksha.shopsphere.payment.service.PaymentService;
import com.rudraksha.shopsphere.payment.entity.OutboxEvent;
import com.rudraksha.shopsphere.payment.repository.OutboxEventRepository;
import com.rudraksha.shopsphere.payment.event.EventEnvelope;
import com.rudraksha.shopsphere.payment.event.PaymentSucceededPayload;
import com.rudraksha.shopsphere.payment.event.PaymentFailedPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    public PaymentResponse processPayment(ProcessPaymentRequest request) {
        String transactionId = generateTransactionId();

        Payment payment = Payment.builder()
                .transactionId(transactionId)
                .orderNumber(request.getOrderNumber())
                .sessionId(request.getSessionId())
                .userId(request.getUserId())
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
            publishPaymentEvent("PAYMENT_SUCCESS", transactionId, savedPayment.getOrderNumber(), savedPayment.getUserId(), savedPayment.getSessionId(), savedPayment.getAmount(), null);
        } else {
            savedPayment.setStatus(Payment.PaymentStatus.FAILED);
            savedPayment.setFailureReason("Payment gateway declined");
            publishPaymentEvent("PAYMENT_FAILED", transactionId, savedPayment.getOrderNumber(), savedPayment.getUserId(), savedPayment.getSessionId(), savedPayment.getAmount(), savedPayment.getFailureReason());
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
    public Page<PaymentResponse> getPaymentsByOrderNumber(String orderNumber, Pageable pageable) {
        return paymentRepository.findByOrderNumber(orderNumber, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByUserId(String userId, Pageable pageable) {
        return paymentRepository.findByUserId(userId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByStatus(Payment.PaymentStatus status, Pageable pageable) {
        return paymentRepository.findByStatus(status, pageable)
                .map(this::mapToResponse);
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
        
        publishPaymentEvent("PAYMENT_REFUNDED", payment.getTransactionId(), payment.getOrderNumber(), payment.getUserId());
        
        return mapToResponse(refundedPayment);
    }

    @Override
    public PaymentResponse updatePaymentStatus(Long id, Payment.PaymentStatus status) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentException("Payment not found with id: " + id));

        payment.setStatus(status);
        Payment updatedPayment = paymentRepository.save(payment);

        publishPaymentEvent("PAYMENT_STATUS_UPDATED", updatedPayment.getTransactionId(), updatedPayment.getOrderNumber(), updatedPayment.getUserId());

        return mapToResponse(updatedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(Pageable pageable) {
        return paymentRepository.findAll(pageable)
                .map(this::mapToResponse);
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
                .orderNumber(payment.getOrderNumber())
                .userId(payment.getUserId())
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

    private void publishPaymentEvent(String eventType, String transactionId, String orderNumber, String userId) {
        publishPaymentEvent(eventType, transactionId, orderNumber, userId, null, null, null);
    }

    private void publishPaymentEvent(String eventType, String transactionId, String orderNumber, String userId, String sessionId, java.math.BigDecimal amount, String failureReason) {
        try {
            // 1. Dual-publish to the old 'payment-events' topic for backward compatibility
            String message = eventType + ":" + transactionId + ":" + orderNumber + ":" + userId + ":" + LocalDateTime.now();
            OutboxEvent oldEvent = OutboxEvent.builder()
                    .topic("payment-events")
                    .key(orderNumber)
                    .payload(message)
                    .build();
            outboxRepository.save(oldEvent);
            log.info("Saved legacy payment outbox event: {} for order {}", eventType, orderNumber);

            // 2. Publish new structured event format
            if ("PAYMENT_SUCCESS".equals(eventType)) {
                PaymentSucceededPayload payload = PaymentSucceededPayload.builder()
                        .sessionId(sessionId)
                        .orderNumber(orderNumber)
                        .userId(userId)
                        .transactionId(transactionId)
                        .amount(amount)
                        .build();

                EventEnvelope<PaymentSucceededPayload> envelope = EventEnvelope.<PaymentSucceededPayload>builder()
                        .type("PAYMENT_SUCCEEDED")
                        .source("payment-service")
                        .payload(payload)
                        .build();

                String jsonPayload = objectMapper.writeValueAsString(envelope);
                OutboxEvent newEvent = OutboxEvent.builder()
                        .topic("payment.succeeded")
                        .key(orderNumber)
                        .payload(jsonPayload)
                        .build();
                outboxRepository.save(newEvent);
                log.info("Saved structured payment outbox event (succeeded) for session {}", sessionId);
            } else if ("PAYMENT_FAILED".equals(eventType)) {
                PaymentFailedPayload payload = PaymentFailedPayload.builder()
                        .sessionId(sessionId)
                        .orderNumber(orderNumber)
                        .userId(userId)
                        .transactionId(transactionId)
                        .reason(failureReason != null ? failureReason : "Payment gateway declined")
                        .build();

                EventEnvelope<PaymentFailedPayload> envelope = EventEnvelope.<PaymentFailedPayload>builder()
                        .type("PAYMENT_FAILED")
                        .source("payment-service")
                        .payload(payload)
                        .build();

                String jsonPayload = objectMapper.writeValueAsString(envelope);
                OutboxEvent newEvent = OutboxEvent.builder()
                        .topic("payment.failed")
                        .key(orderNumber)
                        .payload(jsonPayload)
                        .build();
                outboxRepository.save(newEvent);
                log.info("Saved structured payment outbox event (failed) for session {}", sessionId);
            }
        } catch (Exception e) {
            log.error("Failed to save payment outbox event: {} for order {}", eventType, orderNumber, e);
        }
    }
}
