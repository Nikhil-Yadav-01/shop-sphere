package com.rudraksha.shopsphere.pricing.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceCalculationResponse {
    private String productId;
    private Integer quantity;
    private BigDecimal basePrice;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private BigDecimal discountAmount;
    private BigDecimal discountPercentage;
    private String appliedRules;
    private String appliedTier;
}
