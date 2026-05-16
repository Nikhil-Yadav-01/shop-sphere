package com.rudraksha.shopsphere.checkout.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    @NotNull(message = "Payment method is required")
    private String paymentMethod;

    private String cardToken;
    private String paypalEmail;
}