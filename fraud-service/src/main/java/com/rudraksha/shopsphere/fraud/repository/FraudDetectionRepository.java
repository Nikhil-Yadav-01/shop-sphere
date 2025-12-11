package com.rudraksha.shopsphere.fraud.repository;

import com.rudraksha.shopsphere.fraud.entity.FraudDetection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FraudDetectionRepository extends JpaRepository<FraudDetection, Long> {
    
    Optional<FraudDetection> findByTransactionId(String transactionId);
    
    List<FraudDetection> findByCustomerId(Long customerId);
    
    List<FraudDetection> findByOrderId(Long orderId);
    
    List<FraudDetection> findByIsFraudulent(Boolean isFraudulent);
    
    List<FraudDetection> findByStatus(String status);
    
    @Query("SELECT f FROM FraudDetection f WHERE f.customerId = :customerId AND f.isFraudulent = true")
    List<FraudDetection> findFraudulentTransactionsByCustomer(@Param("customerId") Long customerId);
}
