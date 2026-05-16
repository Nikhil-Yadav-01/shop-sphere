package com.rudraksha.shopsphere.checkout.service.impl;

import com.rudraksha.shopsphere.checkout.dto.request.CheckoutRequest;
import com.rudraksha.shopsphere.checkout.dto.request.OrderItemRequest;
import com.rudraksha.shopsphere.checkout.dto.response.*;
import com.rudraksha.shopsphere.checkout.service.CheckoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import org.springframework.kafka.support.SendResult;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutServiceImpl implements CheckoutService {

    private final CartClient cartClient;
    private final OrderClient orderClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public OrderResponse processCheckout(String userId, CheckoutRequest request) {
        log.info("Processing checkout for user: {}", userId);

        // 1. Get cart items
        var cart = cartClient.getCart(userId);
        if (cart == null || cart.items() == null || cart.items().isEmpty()) {
            throw new IllegalStateException("Cart is empty or cart service is unavailable");
        }

        // 2. Map to Order Creation Request
        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                .userId(userId)
                .totalAmount(calculateTotal(cart))
                .taxAmount(calculateTax(cart))
                .shippingAddress(request.getShippingAddress().getAddressLine1() + ", " + request.getShippingAddress().getCity())
                .billingAddress(request.getShippingAddress().getAddressLine1())
                .items(cart.items().stream()
                        .map(item -> OrderItemRequest.builder()
                                .productId(item.productId())
                                .productName(item.productName())
                                .quantity(item.quantity())
                                .unitPrice(item.price())
                                .totalPrice(item.subtotal())
                                .build())
                        .collect(Collectors.toList()))
                .build();

        // 3. Call Order Service to create the "Legal Record"
        OrderResponse order = orderClient.createOrder(orderRequest);

        // 4. Initiate SAGA asynchronously
        final String orderNumber = order.getOrderNumber();
        CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send("checkout.initiated", orderNumber, order);
        
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish checkout.initiated event for order {}", orderNumber, ex);
            } else {
                log.info("Successfully published checkout.initiated event for order {}", orderNumber);
            }
        });

        return order;
    }

    private BigDecimal calculateTotal(CartClient.CartResponse cart) {
        BigDecimal subtotal = cart.totalPrice();
        return subtotal.add(calculateTax(cart)).add(BigDecimal.valueOf(9.99));
    }

    private BigDecimal calculateTax(CartClient.CartResponse cart) {
        return cart.totalPrice().multiply(BigDecimal.valueOf(0.08));
    }

    @Override
    public OrderResponse getOrder(Long orderId) {
        return orderClient.getOrderById(orderId);
    }

    @Override
    public OrderResponse getOrderByNumber(String orderNumber) {
        return orderClient.getOrderByOrderNumber(orderNumber);
    }

    @Override
    public List<OrderResponse> getUserOrders(String userId) {
        // This would need a paginated version or just call order-service
        return orderClient.getOrdersByUserId(userId, 0, 10).getContent();
    }

    @Override
    public Page<OrderResponse> getUserOrders(String userId, org.springframework.data.domain.Pageable pageable) {
        // We'll adapt the Pageable to simple params for Feign
        var pageResp = orderClient.getOrdersByUserId(userId, pageable.getPageNumber(), pageable.getPageSize());
        return new org.springframework.data.domain.PageImpl<>(pageResp.getContent(), pageable, pageResp.totalElements());
    }

    @Override
    public OrderResponse updateOrderStatus(Long orderId, String status) {
        return orderClient.updateOrderStatus(orderId, status);
    }

    @FeignClient(name = "CART-SERVICE", path = "/api/v1/cart", fallbackFactory = CartClientFallbackFactory.class)
    public interface CartClient {
        @GetMapping
        CartResponse getCart(@RequestHeader("X-User-Id") String userId);
        
        @DeleteMapping
        void clearCart(@RequestHeader("X-User-Id") String userId);

        record CartResponse(String id, String userId, List<CartItemResponse> items, Integer totalItems, BigDecimal totalPrice) {}
        record CartItemResponse(String productId, String productName, Integer quantity, BigDecimal price, BigDecimal subtotal) {}
    }

    @FeignClient(name = "ORDER-SERVICE", path = "/order")
    public interface OrderClient {
        @PostMapping
        OrderResponse createOrder(@RequestBody CreateOrderRequest request);

        @GetMapping("/{id}")
        OrderResponse getOrderById(@PathVariable("id") Long id);

        @GetMapping("/number/{orderNumber}")
        OrderResponse getOrderByOrderNumber(@PathVariable("orderNumber") String orderNumber);

        @GetMapping("/user/{userId}")
        PageResponse<OrderResponse> getOrdersByUserId(@PathVariable("userId") String userId, @RequestParam("page") int page, @RequestParam("size") int size);

        @PutMapping("/{id}/status")
        OrderResponse updateOrderStatus(@PathVariable("id") Long id, @RequestParam("status") String status);
    }

    @lombok.Builder
    @lombok.extern.jackson.Jacksonized
    public record CreateOrderRequest(String userId, BigDecimal totalAmount, BigDecimal taxAmount, String shippingAddress, String billingAddress, List<OrderItemRequest> items) {}
    
    @lombok.Builder
    public record PageResponse<T>(List<T> content, int totalPages, long totalElements) {
        public List<T> getContent() { return content; }
    }
}
