package com.rudraksha.shopsphere.catalog.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "product_variants")
public class ProductVariant {
    @Id
    private String id;

    @Indexed
    private String productId;

    @Indexed(unique = true)
    private String sku;

    private Map<String, String> attributes; // size: "L", color: "Red"

    private String imageUrl;

    private boolean active;
}