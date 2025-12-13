package com.rudraksha.shopsphere.review.controller;

import com.rudraksha.shopsphere.review.dto.request.CreateReviewRequest;
import com.rudraksha.shopsphere.review.dto.request.UpdateReviewRequest;
import com.rudraksha.shopsphere.review.dto.response.ReviewResponse;
import com.rudraksha.shopsphere.review.entity.Review;
import com.rudraksha.shopsphere.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody CreateReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.createReview(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getReviewById(@PathVariable String id) {
        return ResponseEntity.ok(reviewService.getReviewById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> updateReview(@PathVariable String id, @Valid @RequestBody UpdateReviewRequest request) {
        return ResponseEntity.ok(reviewService.updateReview(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable String id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<ReviewResponse>> getAllReviews(Pageable pageable) {
        return ResponseEntity.ok(reviewService.getAllReviews(pageable));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByProductId(@PathVariable String productId, Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviewsByProductId(productId, pageable));
    }

    @GetMapping("/product/{productId}/approved")
    public ResponseEntity<Page<ReviewResponse>> getApprovedReviewsByProductId(@PathVariable String productId, Pageable pageable) {
        return ResponseEntity.ok(reviewService.getApprovedReviewsByProductId(productId, pageable));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByUserId(@PathVariable String userId, Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviewsByUserId(userId, pageable));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByStatus(@PathVariable Review.ReviewStatus status, Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviewsByStatus(status, pageable));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ReviewResponse> approveReview(@PathVariable String id) {
        return ResponseEntity.ok(reviewService.approveReview(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ReviewResponse> rejectReview(@PathVariable String id) {
        return ResponseEntity.ok(reviewService.rejectReview(id));
    }
}
