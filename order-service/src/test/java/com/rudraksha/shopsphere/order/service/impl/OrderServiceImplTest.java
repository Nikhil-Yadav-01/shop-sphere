package com.rudraksha.shopsphere.order.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rudraksha.shopsphere.order.dto.request.CreateOrderRequest;
import com.rudraksha.shopsphere.order.dto.request.OrderItemRequest;
import com.rudraksha.shopsphere.order.dto.response.OrderResponse;
import com.rudraksha.shopsphere.order.entity.Order;
import com.rudraksha.shopsphere.order.entity.OrderItem;
import com.rudraksha.shopsphere.order.exception.OrderException;
import com.rudraksha.shopsphere.order.repository.OrderRepository;
import com.rudraksha.shopsphere.order.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxEventRepository outboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order order;
    private Long orderId = 1L;
    private String orderNumber = "ORD-123";

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .id(orderId)
                .orderNumber(orderNumber)
                .userId("user-123")
                .status(Order.OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(200.00))
                .items(Collections.singletonList(OrderItem.builder().productId("p1").quantity(2).unitPrice(BigDecimal.valueOf(100.00)).totalPrice(BigDecimal.valueOf(200.00)).build()))
                .build();
    }

    @Test
    void createOrder_Success() {
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId("p1");
        itemRequest.setQuantity(2);
        itemRequest.setUnitPrice(BigDecimal.valueOf(100.00));
        itemRequest.setTotalPrice(BigDecimal.valueOf(200.00));

        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId("user-123");
        request.setItems(Collections.singletonList(itemRequest));
        request.setTotalAmount(BigDecimal.valueOf(200.00));

        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderResponse response = orderService.createOrder(request);

        assertNotNull(response);
        assertEquals(Order.OrderStatus.PENDING, response.getStatus());
        verify(orderRepository).save(any(Order.class));
        verify(outboxRepository, atLeastOnce()).save(any());
    }

    @Test
    void getOrderById_Success() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(orderId);

        assertNotNull(response);
        assertEquals(orderId, response.getId());
    }

    @Test
    void getOrderById_NotFound() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(OrderException.class, () -> orderService.getOrderById(orderId));
    }

    @Test
    void updateOrderStatus_Success() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderResponse response = orderService.updateOrderStatus(orderId, Order.OrderStatus.SHIPPED);

        assertNotNull(response);
        assertEquals(Order.OrderStatus.SHIPPED, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void getAllOrders_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(Collections.singletonList(order));
        when(orderRepository.findAll(pageable)).thenReturn(page);

        Page<OrderResponse> response = orderService.getAllOrders(pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
    }
}
