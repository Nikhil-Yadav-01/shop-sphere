package com.rudraksha.shopsphere.payment.repository;

import com.rudraksha.shopsphere.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentId(String paymentId);
    List<Payment> findByOrderId(String orderId);
    List<Payment> findByUserId(String userId);
    List<Payment> findByStatus(Payment.PaymentStatus status);
}