package com.rudraksha.shopsphere.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {
    @NotBlank(message = "User ID is required")
    private String userId;

    @NotNull(message = "Total amount is required")
    private BigDecimal totalAmount;

    @NotNull(message = "Tax amount is required")
    private BigDecimal taxAmount;

    private String shippingAddress;
    private String billingAddress;
    private String checkoutSessionId;

    @NotEmpty(message = "Order must have at least one item")
    private List<OrderItemRequest> items;
}
