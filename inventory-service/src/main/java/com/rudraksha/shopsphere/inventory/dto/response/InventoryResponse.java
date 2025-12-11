package com.rudraksha.shopsphere.inventory.dto.response;

import com.rudraksha.shopsphere.inventory.entity.InventoryItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryResponse {
    private Long id;
    private String sku;
    private Long productId;
    private Integer quantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private Integer reorderLevel;
    private InventoryItem.InventoryStatus status;
    private String warehouseLocation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static InventoryResponse fromEntity(InventoryItem item) {
        return InventoryResponse.builder()
            .id(item.getId())
            .sku(item.getSku())
            .productId(item.getProductId())
            .quantity(item.getQuantity())
            .reservedQuantity(item.getReservedQuantity())
            .availableQuantity(item.getAvailableQuantity())
            .reorderLevel(item.getReorderLevel())
            .status(item.getStatus())
            .warehouseLocation(item.getWarehouseLocation())
            .createdAt(item.getCreatedAt())
            .updatedAt(item.getUpdatedAt())
            .build();
    }
}
