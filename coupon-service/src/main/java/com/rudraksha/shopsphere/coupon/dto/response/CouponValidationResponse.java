package com.rudraksha.shopsphere.coupon.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponValidationResponse {
    private Boolean valid;
    private String message;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String couponCode;
}