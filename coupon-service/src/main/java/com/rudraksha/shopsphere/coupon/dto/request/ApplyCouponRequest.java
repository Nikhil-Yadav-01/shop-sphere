package com.rudraksha.shopsphere.coupon.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyCouponRequest {
    @NotBlank(message = "Coupon code is required")
    private String couponCode;
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotBlank(message = "Order ID is required")
    private String orderId;
    
    @NotNull(message = "Order amount is required")
    @DecimalMin(value = "0.01", message = "Order amount must be greater than 0")
    private BigDecimal orderAmount;
}