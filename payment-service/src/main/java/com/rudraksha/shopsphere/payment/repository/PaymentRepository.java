package com.rudraksha.shopsphere.payment.repository;

import com.rudraksha.shopsphere.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransactionId(String transactionId);
    List<Payment> findByOrderId(Long orderId);
    List<Payment> findByCustomerId(Long customerId);
    List<Payment> findByStatus(Payment.PaymentStatus status);
    Optional<Payment> findByOrderIdAndStatus(Long orderId, Payment.PaymentStatus status);
}
