package com.rudraksha.shopsphere.pricing.dto.request;

import com.rudraksha.shopsphere.pricing.entity.PricingRule;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePricingRuleRequest {
    @NotBlank(message = "Rule name is required")
    @Size(max = 100, message = "Rule name must not exceed 100 characters")
    private String ruleName;
    
    @NotNull(message = "Rule type is required")
    private PricingRule.RuleType ruleType;
    
    @DecimalMin(value = "0", message = "Discount percentage must be non-negative")
    @DecimalMax(value = "100", message = "Discount percentage must not exceed 100")
    private BigDecimal discountPercentage;
    
    @DecimalMin(value = "0", message = "Discount fixed amount must be non-negative")
    private BigDecimal discountFixed;
    
    @Min(value = 1, message = "Min quantity must be at least 1")
    private Integer minQuantity;
    
    @Min(value = 1, message = "Max quantity must be at least 1")
    private Integer maxQuantity;
    
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
}
