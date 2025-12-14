package com.rudraksha.shopsphere.search.kafka;

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
public class ProductEvent {
    private String productId;
    private String productName;
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
    private String eventType;
    private Long timestamp;
}
