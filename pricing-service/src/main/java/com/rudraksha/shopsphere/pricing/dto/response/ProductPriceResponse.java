package com.rudraksha.shopsphere.pricing.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPriceResponse {
    private Long id;
    private String productId;
    private BigDecimal basePrice;
    private String currency;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
