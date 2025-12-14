package com.rudraksha.shopsphere.recommendation.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Document(collection = "recommendations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recommendation {
    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String productId;

    private String productName;

    private String productCategory;

    private Double score;

    private RecommendationType recommendationType;

    private String reason;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum RecommendationType {
        COLLABORATIVE_FILTERING,
        CONTENT_BASED,
        POPULARITY_BASED,
        TRENDING,
        SIMILAR_PRODUCTS
    }
}
