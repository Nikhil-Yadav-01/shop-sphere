package com.rudraksha.shopsphere.pricing.repository;

import com.rudraksha.shopsphere.pricing.entity.PricingTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PricingTierRepository extends JpaRepository<PricingTier, Long> {
    @Query("SELECT t FROM PricingTier t WHERE :quantity >= t.minQuantity " +
           "AND (t.maxQuantity IS NULL OR :quantity <= t.maxQuantity)")
    Optional<PricingTier> findTierByQuantity(@Param("quantity") Integer quantity);
    
    @Query("SELECT t FROM PricingTier t ORDER BY t.minQuantity ASC")
    List<PricingTier> findAllOrderByQuantity();
}
