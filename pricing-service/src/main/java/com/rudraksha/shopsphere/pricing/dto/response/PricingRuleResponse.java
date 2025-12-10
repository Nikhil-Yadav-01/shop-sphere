package com.rudraksha.shopsphere.pricing.dto.response;

import com.rudraksha.shopsphere.pricing.entity.PricingRule;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingRuleResponse {
    private Long id;
    private String ruleName;
    private PricingRule.RuleType ruleType;
    private BigDecimal discountPercentage;
    private BigDecimal discountFixed;
    private Integer minQuantity;
    private Integer maxQuantity;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
