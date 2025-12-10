package com.rudraksha.shopsphere.pricing.repository;

import com.rudraksha.shopsphere.pricing.entity.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {
    List<PricingRule> findByRuleType(PricingRule.RuleType ruleType);
    List<PricingRule> findByIsActiveTrue();
    
    @Query("SELECT r FROM PricingRule r WHERE r.isActive = true " +
           "AND (r.validFrom IS NULL OR r.validFrom <= :now) " +
           "AND (r.validUntil IS NULL OR r.validUntil >= :now) " +
           "AND r.ruleType = :ruleType")
    List<PricingRule> findActiveRulesByType(@Param("ruleType") PricingRule.RuleType ruleType, 
                                            @Param("now") LocalDateTime now);
    
    @Query("SELECT r FROM PricingRule r WHERE r.isActive = true " +
           "AND (r.validFrom IS NULL OR r.validFrom <= :now) " +
           "AND (r.validUntil IS NULL OR r.validUntil >= :now)")
    List<PricingRule> findAllActiveRules(@Param("now") LocalDateTime now);
}
