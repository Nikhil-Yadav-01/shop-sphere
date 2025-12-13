package com.rudraksha.shopsphere.user.repository;

import com.rudraksha.shopsphere.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByAuthUserId(UUID authUserId);

    boolean existsByAuthUserId(UUID authUserId);
}
