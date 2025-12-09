package com.rudraksha.shopsphere.shared.models.dto;

import com.rudraksha.shopsphere.shared.models.enums.OrderStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private UUID id;

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotEmpty(message = "Order must have at least one item")
    private List<OrderItemDTO> items;

    private BigDecimal totalAmount;

    @NotNull(message = "Order status is required")
    private OrderStatus status;

    private Instant createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDTO {
        private UUID productId;
        private String productName;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}
