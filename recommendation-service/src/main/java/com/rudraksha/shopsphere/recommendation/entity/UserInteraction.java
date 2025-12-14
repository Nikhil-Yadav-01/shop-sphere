package com.rudraksha.shopsphere.recommendation.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Document(collection = "user_interactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
    @CompoundIndex(name = "user_product_idx", def = "{'userId': 1, 'productId': 1}")
})
public class UserInteraction {
    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String productId;

    private InteractionType interactionType;

    private Integer interactionScore;

    private String userCategory;

    @CreatedDate
    private LocalDateTime createdAt;

    public enum InteractionType {
        VIEW,
        ADD_TO_CART,
        PURCHASE,
        REVIEW,
        WISHLIST,
        CLICK
    }
}
