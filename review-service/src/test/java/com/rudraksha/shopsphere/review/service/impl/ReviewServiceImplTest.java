package com.rudraksha.shopsphere.review.service.impl;

import com.rudraksha.shopsphere.review.dto.request.CreateReviewRequest;
import com.rudraksha.shopsphere.review.dto.request.UpdateReviewRequest;
import com.rudraksha.shopsphere.review.dto.response.ReviewResponse;
import com.rudraksha.shopsphere.review.entity.Review;
import com.rudraksha.shopsphere.review.repository.ReviewRepository;
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
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private Review review;
    private String reviewId = "rev-123";

    @BeforeEach
    void setUp() {
        review = Review.builder()
                .id(reviewId)
                .productId("prod-123")
                .userId("user-123")
                .rating(5)
                .title("Great Product")
                .comment("Really enjoyed using it.")
                .status(Review.ReviewStatus.PENDING)
                .helpfulCount(0)
                .build();
    }

    @Test
    void createReview_Success() {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setProductId("prod-123");
        request.setUserId("user-123");
        request.setRating(5);
        request.setTitle("Great Product");

        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        ReviewResponse response = reviewService.createReview(request);

        assertNotNull(response);
        assertEquals(Review.ReviewStatus.PENDING.toString(), response.getStatus());
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void getReviewById_Success() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        ReviewResponse response = reviewService.getReviewById(reviewId);

        assertNotNull(response);
        assertEquals(reviewId, response.getId());
    }

    @Test
    void approveReview_Success() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        ReviewResponse response = reviewService.approveReview(reviewId);

        assertNotNull(response);
        assertEquals(Review.ReviewStatus.APPROVED.toString(), response.getStatus());
        verify(reviewRepository).save(review);
    }
}
