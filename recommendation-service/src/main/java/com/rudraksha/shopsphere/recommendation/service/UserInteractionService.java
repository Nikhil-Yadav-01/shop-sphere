package com.rudraksha.shopsphere.recommendation.service;

import com.rudraksha.shopsphere.recommendation.dto.request.UserInteractionRequest;
import com.rudraksha.shopsphere.recommendation.dto.response.UserInteractionResponse;

import java.util.List;

public interface UserInteractionService {
    UserInteractionResponse recordInteraction(UserInteractionRequest request);

    UserInteractionResponse getInteractionById(String id);

    List<UserInteractionResponse> getUserInteractions(String userId);

    List<UserInteractionResponse> getProductInteractions(String productId);

    UserInteractionResponse updateInteraction(String id, UserInteractionRequest request);

    void deleteInteraction(String id);

    void deleteUserInteractions(String userId);

    List<UserInteractionResponse> getInteractionsByType(String type);
}
