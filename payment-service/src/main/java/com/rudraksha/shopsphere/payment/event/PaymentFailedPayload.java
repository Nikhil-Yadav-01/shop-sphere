package com.rudraksha.shopsphere.payment.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentFailedPayload {
    private String sessionId;
    private String orderNumber;
    private String userId;
    private String transactionId;
    private String reason;
}
