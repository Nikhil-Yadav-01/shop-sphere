package com.rudraksha.shopsphere.recommendation.dto.request;

import com.rudraksha.shopsphere.recommendation.entity.UserInteraction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInteractionRequest {
    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Product ID is required")
    private String productId;

    @NotNull(message = "Interaction type is required")
    private UserInteraction.InteractionType interactionType;

    private String userCategory;
}
