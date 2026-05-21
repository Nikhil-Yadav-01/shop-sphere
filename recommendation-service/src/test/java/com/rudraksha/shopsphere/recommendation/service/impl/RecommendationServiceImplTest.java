package com.rudraksha.shopsphere.recommendation.service.impl;

import com.rudraksha.shopsphere.recommendation.dto.request.CreateRecommendationRequest;
import com.rudraksha.shopsphere.recommendation.dto.response.RecommendationResponse;
import com.rudraksha.shopsphere.recommendation.entity.Recommendation;
import com.rudraksha.shopsphere.recommendation.repository.RecommendationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {

    @Mock
    private RecommendationRepository recommendationRepository;

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    private Recommendation recommendation;
    private String recId = "rec-123";

    @BeforeEach
    void setUp() {
        recommendation = Recommendation.builder()
                .id(recId)
                .userId("user-123")
                .productId("prod-123")
                .productName("Test Product")
                .score(0.95)
                .recommendationType(Recommendation.RecommendationType.SIMILAR_PRODUCTS)
                .build();
    }

    @Test
    void createRecommendation_Success() {
        CreateRecommendationRequest request = new CreateRecommendationRequest();
        request.setUserId("user-123");
        request.setProductId("prod-123");
        request.setScore(0.95);
        request.setRecommendationType(Recommendation.RecommendationType.SIMILAR_PRODUCTS);

        when(recommendationRepository.save(any(Recommendation.class))).thenReturn(recommendation);

        RecommendationResponse response = recommendationService.createRecommendation(request);

        assertNotNull(response);
        assertEquals(recId, response.getId());
        verify(recommendationRepository).save(any(Recommendation.class));
    }

    @Test
    void getRecommendationById_Success() {
        when(recommendationRepository.findById(recId)).thenReturn(Optional.of(recommendation));

        RecommendationResponse response = recommendationService.getRecommendationById(recId);

        assertNotNull(response);
        assertEquals(recId, response.getId());
    }

    @Test
    void getRecommendationById_NotFound() {
        when(recommendationRepository.findById(recId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> recommendationService.getRecommendationById(recId));
    }
}
