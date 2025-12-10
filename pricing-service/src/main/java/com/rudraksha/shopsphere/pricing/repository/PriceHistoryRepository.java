package com.rudraksha.shopsphere.pricing.repository;

import com.rudraksha.shopsphere.pricing.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
    List<PriceHistory> findByProductId(String productId);
    List<PriceHistory> findByProductIdOrderByChangedAtDesc(String productId);
}
