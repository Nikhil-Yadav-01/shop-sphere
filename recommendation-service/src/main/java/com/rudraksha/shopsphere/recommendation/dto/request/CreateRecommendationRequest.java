package com.rudraksha.shopsphere.recommendation.dto.request;

import com.rudraksha.shopsphere.recommendation.entity.Recommendation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRecommendationRequest {
    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Product ID is required")
    private String productId;

    @NotBlank(message = "Product name is required")
    private String productName;

    private String productCategory;

    @NotNull(message = "Score is required")
    @Positive(message = "Score must be positive")
    private Double score;

    @NotNull(message = "Recommendation type is required")
    private Recommendation.RecommendationType recommendationType;

    private String reason;
}
