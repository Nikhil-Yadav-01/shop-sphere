package com.rudraksha.shopsphere.order.service;

import com.rudraksha.shopsphere.order.dto.request.CreateOrderRequest;
import com.rudraksha.shopsphere.order.dto.response.OrderResponse;
import com.rudraksha.shopsphere.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request);
    OrderResponse getOrderById(Long id);
    OrderResponse getOrderByOrderNumber(String orderNumber);
    List<OrderResponse> getOrdersByUserId(String userId);
    Page<OrderResponse> getOrdersByUserId(String userId, Pageable pageable);
    List<OrderResponse> getOrdersByStatus(Order.OrderStatus status);
    OrderResponse updateOrderStatus(Long id, Order.OrderStatus newStatus);
    void deleteOrder(Long id);
    Page<OrderResponse> getAllOrders(Pageable pageable);
}
