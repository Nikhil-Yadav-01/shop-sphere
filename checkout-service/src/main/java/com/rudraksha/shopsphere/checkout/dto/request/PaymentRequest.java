package com.rudraksha.shopsphere.checkout.dto.request;

import com.rudraksha.shopsphere.checkout.entity.PaymentDetails;
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
    private PaymentDetails.PaymentMethod paymentMethod;

    private String cardToken;
    private String paypalEmail;
}