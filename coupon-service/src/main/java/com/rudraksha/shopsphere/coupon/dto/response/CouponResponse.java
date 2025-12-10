package com.rudraksha.shopsphere.coupon.dto.response;

import com.rudraksha.shopsphere.coupon.entity.Coupon;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponResponse {
    private Long id;
    private String code;
    private String description;
    private Coupon.DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minimumOrderAmount;
    private BigDecimal maximumDiscountAmount;
    private Integer usageLimit;
    private Integer usedCount;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}