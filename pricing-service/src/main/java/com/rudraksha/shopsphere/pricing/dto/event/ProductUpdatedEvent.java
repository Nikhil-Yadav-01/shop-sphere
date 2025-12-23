package com.rudraksha.shopsphere.pricing.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdatedEvent {
    private String productId;
    private String productName;
    private BigDecimal price;
    private String category;
    private LocalDateTime updatedAt;
}
