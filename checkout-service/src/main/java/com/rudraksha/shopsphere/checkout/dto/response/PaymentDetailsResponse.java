package com.rudraksha.shopsphere.checkout.dto.response;

import com.rudraksha.shopsphere.checkout.entity.PaymentDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDetailsResponse {
    private PaymentDetails.PaymentMethod paymentMethod;
    private String transactionId;
    private PaymentDetails.PaymentStatus paymentStatus;
}