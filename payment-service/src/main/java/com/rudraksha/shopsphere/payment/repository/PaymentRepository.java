package com.rudraksha.shopsphere.payment.repository;

import com.rudraksha.shopsphere.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransactionId(String transactionId);
    List<Payment> findByOrderNumber(String orderNumber);
    List<Payment> findByCustomerId(String customerId);
    List<Payment> findByStatus(Payment.PaymentStatus status);
    Optional<Payment> findByOrderNumberAndStatus(String orderNumber, Payment.PaymentStatus status);
}