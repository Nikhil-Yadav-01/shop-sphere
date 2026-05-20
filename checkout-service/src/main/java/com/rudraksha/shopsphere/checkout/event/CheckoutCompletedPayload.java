package com.rudraksha.shopsphere.checkout.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckoutCompletedPayload {
    private String sessionId;
    private String orderNumber;
    private String userId;
    private BigDecimal totalAmount;
}
