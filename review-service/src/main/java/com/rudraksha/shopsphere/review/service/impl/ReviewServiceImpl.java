package com.rudraksha.shopsphere.review.service.impl;

import com.rudraksha.shopsphere.review.dto.request.CreateReviewRequest;
import com.rudraksha.shopsphere.review.dto.request.UpdateReviewRequest;
import com.rudraksha.shopsphere.review.dto.response.ReviewResponse;
import com.rudraksha.shopsphere.review.entity.Review;
import com.rudraksha.shopsphere.review.repository.ReviewRepository;
import com.rudraksha.shopsphere.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;

    @Override
    public ReviewResponse createReview(CreateReviewRequest request) {
        Review review = Review.builder()
                .productId(request.getProductId())
                .userId(request.getUserId())
                .rating(request.getRating())
                .title(request.getTitle())
                .comment(request.getComment())
                .status(Review.ReviewStatus.PENDING)
                .helpfulCount(0)
                .build();
        Review savedReview = reviewRepository.save(review);
        return ReviewResponse.fromEntity(savedReview);
    }

    @Override
    public ReviewResponse getReviewById(String id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found with id: " + id));
        return ReviewResponse.fromEntity(review);
    }

    @Override
    public ReviewResponse updateReview(String id, UpdateReviewRequest request) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found with id: " + id));

        if (request.getRating() != null) {
            review.setRating(request.getRating());
        }
        if (request.getTitle() != null) {
            review.setTitle(request.getTitle());
        }
        if (request.getComment() != null) {
            review.setComment(request.getComment());
        }

        Review updatedReview = reviewRepository.save(review);
        return ReviewResponse.fromEntity(updatedReview);
    }

    @Override
    public void deleteReview(String id) {
        if (!reviewRepository.existsById(id)) {
            throw new RuntimeException("Review not found with id: " + id);
        }
        reviewRepository.deleteById(id);
    }

    @Override
    public Page<ReviewResponse> getAllReviews(Pageable pageable) {
        return reviewRepository.findAll(pageable)
                .map(ReviewResponse::fromEntity);
    }

    @Override
    public Page<ReviewResponse> getReviewsByProductId(String productId, Pageable pageable) {
        return reviewRepository.findByProductId(productId, pageable)
                .map(ReviewResponse::fromEntity);
    }

    @Override
    public Page<ReviewResponse> getReviewsByUserId(String userId, Pageable pageable) {
        return reviewRepository.findByUserId(userId, pageable)
                .map(ReviewResponse::fromEntity);
    }

    @Override
    public Page<ReviewResponse> getReviewsByStatus(Review.ReviewStatus status, Pageable pageable) {
        return reviewRepository.findByStatus(status, pageable)
                .map(ReviewResponse::fromEntity);
    }

    @Override
    public Page<ReviewResponse> getApprovedReviewsByProductId(String productId, Pageable pageable) {
        return reviewRepository.findByProductIdAndStatus(productId, Review.ReviewStatus.APPROVED, pageable)
                .map(ReviewResponse::fromEntity);
    }

    @Override
    public ReviewResponse approveReview(String id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found with id: " + id));
        review.setStatus(Review.ReviewStatus.APPROVED);
        Review updatedReview = reviewRepository.save(review);
        return ReviewResponse.fromEntity(updatedReview);
    }

    @Override
    public ReviewResponse rejectReview(String id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found with id: " + id));
        review.setStatus(Review.ReviewStatus.REJECTED);
        Review updatedReview = reviewRepository.save(review);
        return ReviewResponse.fromEntity(updatedReview);
    }
}
