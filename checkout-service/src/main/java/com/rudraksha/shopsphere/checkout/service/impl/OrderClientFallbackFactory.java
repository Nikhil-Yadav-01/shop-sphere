package com.rudraksha.shopsphere.checkout.service.impl;

import com.rudraksha.shopsphere.checkout.exception.OrderServiceUnavailableException;
import com.rudraksha.shopsphere.checkout.dto.response.OrderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@Slf4j
public class OrderClientFallbackFactory implements FallbackFactory<CheckoutServiceImpl.OrderClient> {

    @Override
    public CheckoutServiceImpl.OrderClient create(Throwable cause) {
        return new CheckoutServiceImpl.OrderClient() {
            @Override
            public OrderResponse createOrder(CheckoutServiceImpl.CreateOrderRequest request) {
                log.error("Order Service fallback triggered for createOrder due to: {}", cause.getMessage(), cause);
                throw new OrderServiceUnavailableException("Order Service is currently unavailable and order could not be created.", cause);
            }

            @Override
            public OrderResponse getOrderById(Long id) {
                log.error("Order Service fallback triggered for getOrderById for ID: {} due to: {}", id, cause.getMessage(), cause);
                throw new OrderServiceUnavailableException("Order Service is currently unavailable and order details could not be retrieved.", cause);
            }

            @Override
            public OrderResponse getOrderByOrderNumber(String orderNumber) {
                log.error("Order Service fallback triggered for getOrderByOrderNumber for Order Number: {} due to: {}", orderNumber, cause.getMessage(), cause);
                throw new OrderServiceUnavailableException("Order Service is currently unavailable and order details could not be retrieved.", cause);
            }

            @Override
            public CheckoutServiceImpl.PageResponse<OrderResponse> getOrdersByUserId(String userId, int page, int size) {
                log.error("Order Service fallback triggered for getOrdersByUserId for User ID: {} due to: {}", userId, cause.getMessage(), cause);
                // Return a clean empty page response rather than throwing, as a form of graceful degradation for lists
                return new CheckoutServiceImpl.PageResponse<>(Collections.emptyList(), 0, 0L);
            }

            @Override
            public OrderResponse updateOrderStatus(Long id, String status) {
                log.error("Order Service fallback triggered for updateOrderStatus for ID: {} to status: {} due to: {}", id, status, cause.getMessage(), cause);
                throw new OrderServiceUnavailableException("Order Service is currently unavailable and order status could not be updated.", cause);
            }
        };
    }
}
