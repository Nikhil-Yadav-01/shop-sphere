package com.rudraksha.shopsphere.recommendation.dto.response;

import com.rudraksha.shopsphere.recommendation.entity.Recommendation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponse {
    private String id;
    private String userId;
    private String productId;
    private String productName;
    private String productCategory;
    private Double score;
    private Recommendation.RecommendationType recommendationType;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
