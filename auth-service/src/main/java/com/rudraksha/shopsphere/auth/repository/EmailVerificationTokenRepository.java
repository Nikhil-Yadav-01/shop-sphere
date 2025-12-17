package com.rudraksha.shopsphere.auth.repository;

import com.rudraksha.shopsphere.auth.entity.EmailVerificationToken;
import com.rudraksha.shopsphere.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByToken(String token);

    Optional<EmailVerificationToken> findByUser(User user);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken e WHERE e.expiresAt < :now")
    void deleteExpiredTokens(LocalDateTime now);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken e WHERE e.user = :user")
    void deleteByUser(User user);
}