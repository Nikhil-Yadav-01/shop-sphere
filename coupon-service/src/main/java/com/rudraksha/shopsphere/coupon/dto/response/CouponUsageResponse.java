package com.rudraksha.shopsphere.coupon.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponUsageResponse {
    private Long id;
    private String couponCode;
    private String userId;
    private String orderId;
    private BigDecimal discountAmount;
    private LocalDateTime usedAt;
}