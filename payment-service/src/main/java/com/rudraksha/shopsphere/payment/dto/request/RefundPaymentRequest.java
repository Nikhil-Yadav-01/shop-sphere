package com.rudraksha.shopsphere.payment.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundPaymentRequest {
    @NotNull(message = "Transaction ID cannot be null")
    @NotBlank(message = "Transaction ID cannot be blank")
    private String transactionId;

    @DecimalMin(value = "0", message = "Refund amount cannot be negative")
    private BigDecimal refundAmount;

    @NotBlank(message = "Reason cannot be blank")
    private String reason;
}
