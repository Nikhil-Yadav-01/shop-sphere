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
public class UpdateCouponRequest {
    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
    
    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    private BigDecimal discountValue;
    
    @DecimalMin(value = "0.00", message = "Minimum order amount must be non-negative")
    private BigDecimal minimumOrderAmount;
    
    @DecimalMin(value = "0.00", message = "Maximum discount amount must be non-negative")
    private BigDecimal maximumDiscountAmount;
    
    @Min(value = 1, message = "Usage limit must be at least 1")
    private Integer usageLimit;
    
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Boolean active;
}