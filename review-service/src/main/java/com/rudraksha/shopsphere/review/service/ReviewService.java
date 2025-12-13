package com.rudraksha.shopsphere.review.service;

import com.rudraksha.shopsphere.review.dto.request.CreateReviewRequest;
import com.rudraksha.shopsphere.review.dto.request.UpdateReviewRequest;
import com.rudraksha.shopsphere.review.dto.response.ReviewResponse;
import com.rudraksha.shopsphere.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {
    ReviewResponse createReview(CreateReviewRequest request);
    ReviewResponse getReviewById(String id);
    ReviewResponse updateReview(String id, UpdateReviewRequest request);
    void deleteReview(String id);
    Page<ReviewResponse> getAllReviews(Pageable pageable);
    Page<ReviewResponse> getReviewsByProductId(String productId, Pageable pageable);
    Page<ReviewResponse> getReviewsByUserId(String userId, Pageable pageable);
    Page<ReviewResponse> getReviewsByStatus(Review.ReviewStatus status, Pageable pageable);
    Page<ReviewResponse> getApprovedReviewsByProductId(String productId, Pageable pageable);
    ReviewResponse approveReview(String id);
    ReviewResponse rejectReview(String id);
}
