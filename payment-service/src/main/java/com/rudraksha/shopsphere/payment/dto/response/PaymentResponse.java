package com.rudraksha.shopsphere.payment.dto.response;

import com.rudraksha.shopsphere.payment.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private Long id;
    private String transactionId;
    private Long orderId;
    private Long customerId;
    private Payment.PaymentStatus status;
    private Payment.PaymentMethod method;
    private BigDecimal amount;
    private String currency;
    private String paymentGatewayId;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime processedAt;
}
