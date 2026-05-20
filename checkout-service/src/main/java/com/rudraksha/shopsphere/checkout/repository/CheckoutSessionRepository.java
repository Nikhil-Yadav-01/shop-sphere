package com.rudraksha.shopsphere.checkout.repository;

import com.rudraksha.shopsphere.checkout.entity.CheckoutSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CheckoutSessionRepository extends JpaRepository<CheckoutSession, Long> {

    Optional<CheckoutSession> findBySessionId(String sessionId);

    Optional<CheckoutSession> findByIdempotencyKey(String idempotencyKey);

    Optional<CheckoutSession> findByOrderNumber(String orderNumber);

    Page<CheckoutSession> findByUserId(String userId, Pageable pageable);
}
