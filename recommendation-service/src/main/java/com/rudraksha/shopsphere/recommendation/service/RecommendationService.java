package com.rudraksha.shopsphere.recommendation.service;

import com.rudraksha.shopsphere.recommendation.dto.request.CreateRecommendationRequest;
import com.rudraksha.shopsphere.recommendation.dto.response.RecommendationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RecommendationService {
    RecommendationResponse createRecommendation(CreateRecommendationRequest request);

    RecommendationResponse getRecommendationById(String id);

    Page<RecommendationResponse> getUserRecommendations(String userId, Pageable pageable);

    List<RecommendationResponse> getTopRecommendationsForUser(String userId, int limit);

    Page<RecommendationResponse> getRecommendationsByType(String type, Pageable pageable);

    Page<RecommendationResponse> getRecommendationsByCategory(String category, Pageable pageable);

    RecommendationResponse updateRecommendation(String id, CreateRecommendationRequest request);

    void deleteRecommendation(String id);

    void deleteUserRecommendations(String userId);

    Page<RecommendationResponse> getAllRecommendations(Pageable pageable);
}
