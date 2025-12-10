package com.rudraksha.shopsphere.payment.dto.request;

import com.rudraksha.shopsphere.payment.entity.Payment;
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
public class ProcessPaymentRequest {
    @NotNull(message = "Order ID cannot be null")
    @Positive(message = "Order ID must be positive")
    private Long orderId;

    @NotNull(message = "Customer ID cannot be null")
    @Positive(message = "Customer ID must be positive")
    private Long customerId;

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Currency cannot be blank")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter code")
    private String currency;

    @NotNull(message = "Payment method cannot be null")
    private Payment.PaymentMethod method;

    @NotBlank(message = "Payment method details cannot be blank")
    private String paymentMethodDetails;
}
