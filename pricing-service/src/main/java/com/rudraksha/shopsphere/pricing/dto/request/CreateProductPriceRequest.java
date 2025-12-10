package com.rudraksha.shopsphere.pricing.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductPriceRequest {
    @NotBlank(message = "Product ID is required")
    private String productId;
    
    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.01", message = "Base price must be greater than 0")
    private BigDecimal basePrice;
    
    @Builder.Default
    private String currency = "USD";
}
