package com.rudraksha.shopsphere.recommendation.dto.response;

import com.rudraksha.shopsphere.recommendation.entity.UserInteraction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInteractionResponse {
    private String id;
    private String userId;
    private String productId;
    private UserInteraction.InteractionType interactionType;
    private Integer interactionScore;
    private String userCategory;
    private LocalDateTime createdAt;
}
