package com.rudraksha.shopsphere.notification.repository;

import com.rudraksha.shopsphere.notification.entity.UserPushToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPushTokenRepository extends JpaRepository<UserPushToken, Long> {
    List<UserPushToken> findByUserId(String userId);
    Optional<UserPushToken> findByUserIdAndToken(String userId, String token);
}
