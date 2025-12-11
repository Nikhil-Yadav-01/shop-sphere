package com.rudraksha.shopsphere.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdjustInventoryRequest {
    @NotNull(message = "Adjustment quantity is required")
    private Integer adjustmentQuantity;

    private String reason;

    private String notes;
}
