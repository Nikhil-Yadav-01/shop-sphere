package com.rudraksha.shopsphere.review.repository;

import com.rudraksha.shopsphere.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {
    Page<Review> findByProductId(String productId, Pageable pageable);
    Page<Review> findByUserId(String userId, Pageable pageable);
    Page<Review> findByStatus(Review.ReviewStatus status, Pageable pageable);
    Page<Review> findByProductIdAndStatus(String productId, Review.ReviewStatus status, Pageable pageable);
}
