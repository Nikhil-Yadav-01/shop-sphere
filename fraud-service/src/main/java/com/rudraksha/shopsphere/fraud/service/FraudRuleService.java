package com.rudraksha.shopsphere.fraud.service;

import com.rudraksha.shopsphere.fraud.entity.FraudRule;
import com.rudraksha.shopsphere.fraud.repository.FraudRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class FraudRuleService {
    
    private final FraudRuleRepository fraudRuleRepository;
    
    public List<FraudRule> getAllEnabledRules() {
        return fraudRuleRepository.findByEnabled(true);
    }
    
    public FraudRule createRule(FraudRule rule) {
        log.info("Creating fraud rule: {}", rule.getRuleName());
        return fraudRuleRepository.save(rule);
    }
    
    public FraudRule updateRule(Long id, FraudRule updatedRule) {
        return fraudRuleRepository.findById(id)
                .map(existingRule -> {
                    existingRule.setRuleName(updatedRule.getRuleName());
                    existingRule.setRuleType(updatedRule.getRuleType());
                    existingRule.setThreshold(updatedRule.getThreshold());
                    existingRule.setEnabled(updatedRule.getEnabled());
                    return fraudRuleRepository.save(existingRule);
                })
                .orElseThrow(() -> new RuntimeException("Rule not found"));
    }
    
    public void deleteRule(Long id) {
        fraudRuleRepository.deleteById(id);
    }
    
    public FraudRule getRuleById(Long id) {
        return fraudRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rule not found"));
    }
}
