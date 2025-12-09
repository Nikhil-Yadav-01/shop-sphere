package com.rudraksha.shopsphere.catalog.dto.request;

import com.rudraksha.shopsphere.catalog.entity.Product.ProductStatus;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductRequest {
    private String name;
    private String description;
    @Positive(message = "Price must be positive")
    private BigDecimal price;
    private String categoryId;
    private List<String> images;
    private ProductStatus status;
}
