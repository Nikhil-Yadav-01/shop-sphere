package com.rudraksha.shopsphere.search.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {
    private String id;

    private String productId;

    private String name;

    private String description;

    private String sku;

    private BigDecimal price;

    private String categoryId;

    private String categoryName;

    private List<String> tags;

    private String status;

    private BigDecimal rating;

    private Integer reviewCount;

    private Boolean inStock;

    private String brand;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
