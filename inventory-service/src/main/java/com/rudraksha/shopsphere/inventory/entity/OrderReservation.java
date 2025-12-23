package com.rudraksha.shopsphere.inventory.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_reservations", indexes = {
    @Index(name = "idx_order_id", columnList = "order_id"),
    @Index(name = "idx_inventory_item_id", columnList = "inventory_item_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private Long inventoryItemId;

    @Column(nullable = false)
    private Integer quantityReserved;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
