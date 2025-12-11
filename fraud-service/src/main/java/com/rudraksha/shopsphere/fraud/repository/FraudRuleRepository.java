package com.rudraksha.shopsphere.fraud.repository;

import com.rudraksha.shopsphere.fraud.entity.FraudRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FraudRuleRepository extends JpaRepository<FraudRule, Long> {
    
    Optional<FraudRule> findByRuleName(String ruleName);
    
    List<FraudRule> findByEnabled(Boolean enabled);
}
