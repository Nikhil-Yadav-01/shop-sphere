package com.rudraksha.shopsphere.payment.service.impl;

import com.rudraksha.shopsphere.payment.dto.request.ProcessPaymentRequest;
import com.rudraksha.shopsphere.payment.dto.request.RefundPaymentRequest;
import com.rudraksha.shopsphere.payment.dto.response.PaymentResponse;
import com.rudraksha.shopsphere.payment.entity.Payment;
import com.rudraksha.shopsphere.payment.exception.PaymentException;
import com.rudraksha.shopsphere.payment.repository.PaymentRepository;
import com.rudraksha.shopsphere.payment.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OutboxEventRepository outboxRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Payment payment;
    private Long paymentId = 1L;
    private String transactionId = "TXN-123";

    @BeforeEach
    void setUp() {
        payment = Payment.builder()
                .id(paymentId)
                .transactionId(transactionId)
                .orderNumber("ORD-123")
                .userId("user-123")
                .status(Payment.PaymentStatus.SUCCESS)
                .amount(BigDecimal.valueOf(100.00))
                .build();
    }

    @Test
    void processPayment_Success() {
        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setOrderNumber("ORD-123");
        request.setUserId("user-123");
        request.setAmount(BigDecimal.valueOf(100.00));
        request.setMethod(Payment.PaymentMethod.CREDIT_CARD);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.processPayment(request);

        assertNotNull(response);
        assertNotNull(response.getTransactionId());
        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(outboxRepository).save(any());
    }

    @Test
    void getPaymentById_Success() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPaymentById(paymentId);

        assertNotNull(response);
        assertEquals(paymentId, response.getId());
    }

    @Test
    void refundPayment_Success() {
        RefundPaymentRequest request = new RefundPaymentRequest();
        request.setTransactionId(transactionId);

        when(paymentRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentResponse response = paymentService.refundPayment(request);

        assertNotNull(response);
        assertEquals(Payment.PaymentStatus.REFUNDED, payment.getStatus());
        verify(outboxRepository).save(any());
    }

    @Test
    void refundPayment_NotSuccessful() {
        payment.setStatus(Payment.PaymentStatus.FAILED);
        RefundPaymentRequest request = new RefundPaymentRequest();
        request.setTransactionId(transactionId);

        when(paymentRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(payment));

        assertThrows(PaymentException.class, () -> paymentService.refundPayment(request));
    }
}
