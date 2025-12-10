package com.rudraksha.shopsphere.payment.dto.response;

import com.rudraksha.shopsphere.payment.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private String paymentId;
    private String orderId;
    private String userId;
    private BigDecimal amount;
    private Payment.PaymentMethod method;
    private Payment.PaymentStatus status;
    private String transactionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}