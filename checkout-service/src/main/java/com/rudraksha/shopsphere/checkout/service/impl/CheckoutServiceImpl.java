package com.rudraksha.shopsphere.checkout.service.impl;

import com.rudraksha.shopsphere.checkout.dto.request.CheckoutRequest;
import com.rudraksha.shopsphere.checkout.dto.response.*;
import com.rudraksha.shopsphere.checkout.entity.*;
import com.rudraksha.shopsphere.checkout.repository.OrderRepository;
import com.rudraksha.shopsphere.checkout.service.CheckoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Component;
import java.util.Collections;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import org.springframework.kafka.support.SendResult;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CheckoutServiceImpl implements CheckoutService {

    private final OrderRepository orderRepository;
    private final CartClient cartClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public OrderResponse processCheckout(String userId, CheckoutRequest request) {
        log.info("Processing checkout for user: {}", userId);

        // Get cart items
        var cart = cartClient.getCart(userId);
        if (cart == null || cart.items() == null || cart.items().isEmpty()) {
            throw new IllegalStateException("Cart is empty or cart service is unavailable");
        }

        // Calculate amounts
        BigDecimal subtotal = cart.totalPrice();
        BigDecimal taxAmount = subtotal.multiply(BigDecimal.valueOf(0.08)); // 8% tax
        BigDecimal shippingAmount = BigDecimal.valueOf(9.99);
        BigDecimal totalAmount = subtotal.add(taxAmount).add(shippingAmount);

        // Create robust Order ID
        String orderNumber = "ORD-" + UUID.randomUUID().toString().replace("-", "").toUpperCase().substring(0, 16);

        // Create order
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .userId(userId)
                .status(Order.OrderStatus.PROCESSING)
                .totalAmount(totalAmount)
                .taxAmount(taxAmount)
                .shippingAmount(shippingAmount)
                .shippingAddress(mapToShippingAddress(request.getShippingAddress()))
                .build();

        // Save order first to get ID
        Order savedOrder = orderRepository.save(order);
        
        // Create order items
        List<OrderItem> orderItems = cart.items().stream()
                .map(item -> OrderItem.builder()
                        .order(savedOrder)
                        .productId(item.productId())
                        .productName(item.productName())
                        .quantity(item.quantity())
                        .unitPrice(item.price())
                        .totalPrice(item.subtotal())
                        .build())
                .collect(Collectors.toList());
        savedOrder.setItems(orderItems);
        order = savedOrder;

        // Publish checkout initiated event asynchronously
        // In a real SAGA, this would use an Outbox table. For simplicity, we just use KafkaTemplate here.
        final String finalOrderNumber = order.getOrderNumber();
        CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send("checkout.initiated", finalOrderNumber, order);
        
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish checkout.initiated event for order {}", finalOrderNumber, ex);
                // In production, save to outbox table here for retry
            } else {
                log.info("Successfully published checkout.initiated event for order {}", finalOrderNumber);
            }
        });

        return mapToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        return mapToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        return mapToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(String userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(String userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable)
                .map(this::mapToOrderResponse);
    }

    @Override
    public OrderResponse updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        
        order.setStatus(Order.OrderStatus.valueOf(status.toUpperCase()));
        order = orderRepository.save(order);
        
        log.info("Updated order {} status to {}", order.getOrderNumber(), status);
        return mapToOrderResponse(order);
    }

    private ShippingAddress mapToShippingAddress(com.rudraksha.shopsphere.checkout.dto.request.ShippingAddressRequest request) {
        return ShippingAddress.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .phone(request.getPhone())
                .build();
    }

    private OrderResponse mapToOrderResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .taxAmount(order.getTaxAmount())
                .shippingAmount(order.getShippingAmount())
                .items(order.getItems().stream().map(this::mapToOrderItemResponse).collect(Collectors.toList()))
                .shippingAddress(mapToShippingAddressResponse(order.getShippingAddress()))
                .transactionId(order.getTransactionId())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderItemResponse mapToOrderItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .build();
    }

    private ShippingAddressResponse mapToShippingAddressResponse(ShippingAddress address) {
        return ShippingAddressResponse.builder()
                .firstName(address.getFirstName())
                .lastName(address.getLastName())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .phone(address.getPhone())
                .build();
    }

    @FeignClient(name = "CART-SERVICE", path = "/api/v1/cart", fallbackFactory = CartClientFallbackFactory.class)
    public interface CartClient {
        @GetMapping
        CartResponse getCart(@RequestHeader("X-User-Id") String userId);
        
        @DeleteMapping
        void clearCart(@RequestHeader("X-User-Id") String userId);

        record CartResponse(
                String id,
                String userId,
                List<CartItemResponse> items,
                Integer totalItems,
                BigDecimal totalPrice,
                LocalDateTime createdAt,
                LocalDateTime updatedAt
        ) {}

        record CartItemResponse(
                String productId,
                String productName,
                Integer quantity,
                BigDecimal price,
                BigDecimal subtotal,
                String imageUrl
        ) {}
    }
}