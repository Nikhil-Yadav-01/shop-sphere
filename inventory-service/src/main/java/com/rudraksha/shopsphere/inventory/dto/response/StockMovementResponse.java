package com.rudraksha.shopsphere.inventory.dto.response;

import com.rudraksha.shopsphere.inventory.entity.StockMovement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovementResponse {
    private Long id;
    private Long inventoryItemId;
    private StockMovement.MovementType movementType;
    private Integer quantity;
    private String reference;
    private String notes;
    private LocalDateTime createdAt;

    public static StockMovementResponse fromEntity(StockMovement movement) {
        return StockMovementResponse.builder()
            .id(movement.getId())
            .inventoryItemId(movement.getInventoryItemId())
            .movementType(movement.getMovementType())
            .quantity(movement.getQuantity())
            .reference(movement.getReference())
            .notes(movement.getNotes())
            .createdAt(movement.getCreatedAt())
            .build();
    }
}
