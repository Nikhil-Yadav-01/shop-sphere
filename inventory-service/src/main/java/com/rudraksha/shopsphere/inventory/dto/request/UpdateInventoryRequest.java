package com.rudraksha.shopsphere.inventory.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateInventoryRequest {
    @Positive(message = "Quantity must be greater than 0")
    private Integer quantity;

    @Positive(message = "Reorder level must be greater than 0")
    private Integer reorderLevel;

    private String warehouseLocation;
}
