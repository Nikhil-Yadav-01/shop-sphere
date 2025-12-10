package com.rudraksha.shopsphere.checkout.service;

import com.rudraksha.shopsphere.checkout.dto.request.CheckoutRequest;
import com.rudraksha.shopsphere.checkout.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CheckoutService {
    OrderResponse processCheckout(String userId, CheckoutRequest request);
    OrderResponse getOrder(Long orderId);
    OrderResponse getOrderByNumber(String orderNumber);
    List<OrderResponse> getUserOrders(String userId);
    Page<OrderResponse> getUserOrders(String userId, Pageable pageable);
    OrderResponse updateOrderStatus(Long orderId, String status);
}