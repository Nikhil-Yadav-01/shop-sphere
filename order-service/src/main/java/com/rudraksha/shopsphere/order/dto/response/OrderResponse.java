package com.rudraksha.shopsphere.order.dto.response;

import com.rudraksha.shopsphere.order.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private Long customerId;
    private Order.OrderStatus status;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private String shippingAddress;
    private String billingAddress;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
