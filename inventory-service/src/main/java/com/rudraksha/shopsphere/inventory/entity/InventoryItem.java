package com.rudraksha.shopsphere.inventory.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Formula;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_items", indexes = {
    @Index(name = "idx_product_id", columnList = "product_id"),
    @Index(name = "idx_sku", columnList = "sku")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer reservedQuantity;

    @Formula("quantity - reserved_quantity")
    private Integer availableQuantity;

    @Column(nullable = false)
    private Integer reorderLevel;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private InventoryStatus status;

    @Column
    private String warehouseLocation;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        status = InventoryStatus.AVAILABLE;
        reservedQuantity = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum InventoryStatus {
        AVAILABLE, LOW_STOCK, OUT_OF_STOCK, DISCONTINUED
    }
}
