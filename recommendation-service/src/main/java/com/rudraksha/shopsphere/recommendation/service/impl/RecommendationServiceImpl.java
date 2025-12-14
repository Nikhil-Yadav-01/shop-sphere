package com.rudraksha.shopsphere.recommendation.service.impl;

import com.rudraksha.shopsphere.recommendation.dto.request.CreateRecommendationRequest;
import com.rudraksha.shopsphere.recommendation.dto.response.RecommendationResponse;
import com.rudraksha.shopsphere.recommendation.entity.Recommendation;
import com.rudraksha.shopsphere.recommendation.repository.RecommendationRepository;
import com.rudraksha.shopsphere.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final RecommendationRepository recommendationRepository;

    @Override
    public RecommendationResponse createRecommendation(CreateRecommendationRequest request) {
        Recommendation recommendation = Recommendation.builder()
                .userId(request.getUserId())
                .productId(request.getProductId())
                .productName(request.getProductName())
                .productCategory(request.getProductCategory())
                .score(request.getScore())
                .recommendationType(request.getRecommendationType())
                .reason(request.getReason())
                .build();

        Recommendation saved = recommendationRepository.save(recommendation);
        return mapToResponse(saved);
    }

    @Override
    public RecommendationResponse getRecommendationById(String id) {
        Recommendation recommendation = recommendationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation not found with id: " + id));
        return mapToResponse(recommendation);
    }

    @Override
    public Page<RecommendationResponse> getUserRecommendations(String userId, Pageable pageable) {
        Page<Recommendation> page = recommendationRepository.findByUserId(userId, pageable);
        return new PageImpl<>(
                page.getContent().stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList()),
                pageable,
                page.getTotalElements()
        );
    }

    @Override
    public List<RecommendationResponse> getTopRecommendationsForUser(String userId, int limit) {
        List<Recommendation> recommendations = recommendationRepository.findByUserIdOrderByScoreDesc(userId);
        return recommendations.stream()
                .limit(limit)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<RecommendationResponse> getRecommendationsByType(String type, Pageable pageable) {
        Recommendation.RecommendationType recType = Recommendation.RecommendationType.valueOf(type.toUpperCase());
        Page<Recommendation> page = recommendationRepository.findByRecommendationType(recType, pageable);
        return new PageImpl<>(
                page.getContent().stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList()),
                pageable,
                page.getTotalElements()
        );
    }

    @Override
    public Page<RecommendationResponse> getRecommendationsByCategory(String category, Pageable pageable) {
        Page<Recommendation> page = recommendationRepository.findByProductCategory(category, pageable);
        return new PageImpl<>(
                page.getContent().stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList()),
                pageable,
                page.getTotalElements()
        );
    }

    @Override
    public RecommendationResponse updateRecommendation(String id, CreateRecommendationRequest request) {
        Recommendation recommendation = recommendationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation not found with id: " + id));

        recommendation.setProductName(request.getProductName());
        recommendation.setProductCategory(request.getProductCategory());
        recommendation.setScore(request.getScore());
        recommendation.setRecommendationType(request.getRecommendationType());
        recommendation.setReason(request.getReason());

        Recommendation updated = recommendationRepository.save(recommendation);
        return mapToResponse(updated);
    }

    @Override
    public void deleteRecommendation(String id) {
        recommendationRepository.deleteById(id);
    }

    @Override
    public void deleteUserRecommendations(String userId) {
        recommendationRepository.deleteByUserId(userId);
    }

    @Override
    public Page<RecommendationResponse> getAllRecommendations(Pageable pageable) {
        Page<Recommendation> page = recommendationRepository.findAll(pageable);
        return new PageImpl<>(
                page.getContent().stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList()),
                pageable,
                page.getTotalElements()
        );
    }

    private RecommendationResponse mapToResponse(Recommendation recommendation) {
        return RecommendationResponse.builder()
                .id(recommendation.getId())
                .userId(recommendation.getUserId())
                .productId(recommendation.getProductId())
                .productName(recommendation.getProductName())
                .productCategory(recommendation.getProductCategory())
                .score(recommendation.getScore())
                .recommendationType(recommendation.getRecommendationType())
                .reason(recommendation.getReason())
                .createdAt(recommendation.getCreatedAt())
                .updatedAt(recommendation.getUpdatedAt())
                .build();
    }
}
