package com.rudraksha.shopsphere.search.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {
    @NotBlank(message = "Keyword is required")
    private String keyword;

    private String categoryId;

    private String status;

    private Boolean inStock;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private String sortBy;

    private String sortDirection;
}
