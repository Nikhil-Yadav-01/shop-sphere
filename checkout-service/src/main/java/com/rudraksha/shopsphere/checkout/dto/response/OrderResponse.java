package com.rudraksha.shopsphere.checkout.dto.response;

import com.rudraksha.shopsphere.checkout.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private String userId;
    private Order.OrderStatus status;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private List<OrderItemResponse> items;
    private ShippingAddressResponse shippingAddress;
    private PaymentDetailsResponse paymentDetails;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}