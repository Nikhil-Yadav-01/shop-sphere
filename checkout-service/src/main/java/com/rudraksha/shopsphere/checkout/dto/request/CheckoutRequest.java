package com.rudraksha.shopsphere.checkout.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {
    @Valid
    @NotNull(message = "Shipping address is required")
    private ShippingAddressRequest shippingAddress;

    @Valid
    @NotNull(message = "Payment details are required")
    private PaymentRequest payment;
}