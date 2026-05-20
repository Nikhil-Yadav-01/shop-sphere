package com.rudraksha.shopsphere.order.service.impl;

import com.rudraksha.shopsphere.order.dto.request.CreateOrderRequest;
import com.rudraksha.shopsphere.order.dto.request.OrderItemRequest;
import com.rudraksha.shopsphere.order.dto.response.OrderItemResponse;
import com.rudraksha.shopsphere.order.dto.response.OrderResponse;
import com.rudraksha.shopsphere.order.entity.Order;
import com.rudraksha.shopsphere.order.entity.OrderItem;
import com.rudraksha.shopsphere.order.exception.OrderException;
import com.rudraksha.shopsphere.order.repository.OrderRepository;
import com.rudraksha.shopsphere.order.service.OrderService;
import com.rudraksha.shopsphere.order.entity.OutboxEvent;
import com.rudraksha.shopsphere.order.repository.OutboxEventRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@AllArgsConstructor
@Transactional
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {
        String orderNumber = generateOrderNumber();

        List<OrderItem> items = request.getItems().stream()
                .map(itemRequest -> OrderItem.builder()
                        .productId(itemRequest.getProductId())
                        .productName(itemRequest.getProductName())
                        .quantity(itemRequest.getQuantity())
                        .unitPrice(itemRequest.getUnitPrice())
                        .totalPrice(itemRequest.getTotalPrice())
                        .build())
                .collect(Collectors.toList());

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .userId(request.getUserId())
                .status(Order.OrderStatus.PENDING)
                .totalAmount(request.getTotalAmount())
                .taxAmount(request.getTaxAmount())
                .shippingAddress(request.getShippingAddress())
                .billingAddress(request.getBillingAddress())
                .checkoutSessionId(request.getCheckoutSessionId())
                .items(items)
                .build();

        items.forEach(item -> item.setOrder(order));
        Order savedOrder = orderRepository.save(order);

        publishOrderPlacedEvent(savedOrder);

        return mapToResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderException("Order not found with id: " + id));
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderException("Order not found with order number: " + orderNumber));
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(String userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByUserId(String userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByStatus(Order.OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public OrderResponse updateOrderStatus(Long id, Order.OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderException("Order not found with id: " + id));

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        publishGenericOrderEvent("ORDER_STATUS_UPDATED", order.getOrderNumber() + ":" + newStatus);

        return mapToResponse(updatedOrder);
    }

    @Override
    public void deleteOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderException("Order not found with id: " + id));
        String orderNumber = order.getOrderNumber();
        orderRepository.delete(order);
        publishGenericOrderEvent("ORDER_DELETED", orderNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().replace("-", "").toUpperCase().substring(0, 16);
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getTotalPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .taxAmount(order.getTaxAmount())
                .shippingAddress(order.getShippingAddress())
                .billingAddress(order.getBillingAddress())
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private void publishOrderPlacedEvent(Order order) {
        try {
            List<com.rudraksha.shopsphere.order.event.OrderPlacedPayload.OrderItemPayload> items = order.getItems().stream()
                    .map(item -> com.rudraksha.shopsphere.order.event.OrderPlacedPayload.OrderItemPayload.builder()
                            .sku(item.getProductId())
                            .quantity(item.getQuantity())
                            .build())
                    .collect(Collectors.toList());

            com.rudraksha.shopsphere.order.event.OrderPlacedPayload payload = com.rudraksha.shopsphere.order.event.OrderPlacedPayload.builder()
                    .orderNumber(order.getOrderNumber())
                    .sessionId(order.getCheckoutSessionId())
                    .userId(order.getUserId())
                    .totalAmount(order.getTotalAmount())
                    .items(items)
                    .build();

            com.rudraksha.shopsphere.order.event.EventEnvelope<com.rudraksha.shopsphere.order.event.OrderPlacedPayload> envelope =
                    com.rudraksha.shopsphere.order.event.EventEnvelope.<com.rudraksha.shopsphere.order.event.OrderPlacedPayload>builder()
                            .type("ORDER_PLACED")
                            .source("order-service")
                            .payload(payload)
                            .build();

            String jsonPayload = objectMapper.writeValueAsString(envelope);
            
            OutboxEvent event = OutboxEvent.builder()
                    .topic("order.placed")
                    .key(order.getOrderNumber())
                    .payload(jsonPayload)
                    .build();
            outboxRepository.save(event);
            log.info("Saved outbox event for order.placed: order {} for session {}", order.getOrderNumber(), order.getCheckoutSessionId());
        } catch (Exception e) {
            log.error("Failed to serialize and save order.placed outbox event", e);
        }
    }

    private void publishGenericOrderEvent(String eventType, String details) {
        try {
            String message = eventType + ":" + details + ":" + LocalDateTime.now();
            OutboxEvent event = OutboxEvent.builder()
                    .topic("order-events")
                    .key(details)
                    .payload(message)
                    .build();
            outboxRepository.save(event);
            log.info("Saved outbox event for order-events: {} for {}", eventType, details);
        } catch (Exception e) {
            log.error("Failed to save generic order outbox event", e);
        }
    }
}
