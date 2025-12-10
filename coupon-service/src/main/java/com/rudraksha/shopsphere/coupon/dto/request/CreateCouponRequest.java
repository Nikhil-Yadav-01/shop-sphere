package com.rudraksha.shopsphere.coupon.dto.request;

import com.rudraksha.shopsphere.coupon.entity.Coupon;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCouponRequest {
    @NotBlank(message = "Coupon code is required")
    @Size(min = 3, max = 20, message = "Coupon code must be between 3 and 20 characters")
    private String code;
    
    @NotBlank(message = "Description is required")
    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
    
    @NotNull(message = "Discount type is required")
    private Coupon.DiscountType discountType;
    
    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    private BigDecimal discountValue;
    
    @DecimalMin(value = "0.00", message = "Minimum order amount must be non-negative")
    private BigDecimal minimumOrderAmount;
    
    @DecimalMin(value = "0.00", message = "Maximum discount amount must be non-negative")
    private BigDecimal maximumDiscountAmount;
    
    @NotNull(message = "Usage limit is required")
    @Min(value = 1, message = "Usage limit must be at least 1")
    private Integer usageLimit;
    
    @NotNull(message = "Valid from date is required")
    private LocalDateTime validFrom;
    
    @NotNull(message = "Valid until date is required")
    private LocalDateTime validUntil;
}