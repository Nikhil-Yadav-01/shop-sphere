package com.rudraksha.shopsphere.payment.repository;

import com.rudraksha.shopsphere.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransactionId(String transactionId);
    Page<Payment> findByOrderNumber(String orderNumber, Pageable pageable);
    Page<Payment> findByUserId(String userId, Pageable pageable);
    Page<Payment> findByStatus(Payment.PaymentStatus status, Pageable pageable);
    Optional<Payment> findByOrderNumberAndStatus(String orderNumber, Payment.PaymentStatus status);
}
