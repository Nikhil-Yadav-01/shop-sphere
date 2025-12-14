package com.rudraksha.shopsphere.search.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "search_index")
public class SearchIndex {
    @Id
    private String id;

    @Indexed(unique = true)
    private String productId;

    @TextIndexed
    private String name;

    @TextIndexed
    private String description;

    @Indexed
    private String sku;

    private BigDecimal price;

    @Indexed
    private String categoryId;

    @Indexed
    private String categoryName;

    @TextIndexed
    private List<String> tags;

    @Indexed
    private String status;

    private BigDecimal rating;

    @Indexed
    private Integer reviewCount;

    @Indexed
    private Boolean inStock;

    @TextIndexed
    private String brand;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Indexed
    private Long indexedAt;
}
