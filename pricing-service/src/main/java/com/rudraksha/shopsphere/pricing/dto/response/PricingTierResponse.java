package com.rudraksha.shopsphere.pricing.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingTierResponse {
    private Long id;
    private String tierName;
    private Integer minQuantity;
    private Integer maxQuantity;
    private BigDecimal discountPercentage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
