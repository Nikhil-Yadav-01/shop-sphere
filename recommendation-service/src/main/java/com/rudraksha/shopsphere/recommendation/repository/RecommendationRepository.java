package com.rudraksha.shopsphere.recommendation.repository;

import com.rudraksha.shopsphere.recommendation.entity.Recommendation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationRepository extends MongoRepository<Recommendation, String> {
    Page<Recommendation> findByUserId(String userId, Pageable pageable);

    List<Recommendation> findByUserIdOrderByScoreDesc(String userId);

    Page<Recommendation> findByRecommendationType(Recommendation.RecommendationType type, Pageable pageable);

    Page<Recommendation> findByProductCategory(String category, Pageable pageable);

    List<Recommendation> findByProductId(String productId);

    Long deleteByUserId(String userId);
}
