package com.rudraksha.shopsphere.recommendation.repository;

import com.rudraksha.shopsphere.recommendation.entity.UserInteraction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserInteractionRepository extends MongoRepository<UserInteraction, String> {
    List<UserInteraction> findByUserId(String userId);

    List<UserInteraction> findByProductId(String productId);

    Optional<UserInteraction> findByUserIdAndProductId(String userId, String productId);

    List<UserInteraction> findByInteractionType(UserInteraction.InteractionType type);

    Long deleteByUserId(String userId);
}
