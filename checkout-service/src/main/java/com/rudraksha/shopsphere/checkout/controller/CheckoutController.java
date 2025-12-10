package com.rudraksha.shopsphere.checkout.controller;

import com.rudraksha.shopsphere.checkout.dto.request.CheckoutRequest;
import com.rudraksha.shopsphere.checkout.dto.response.OrderResponse;
import com.rudraksha.shopsphere.checkout.service.CheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping
    public ResponseEntity<OrderResponse> processCheckout(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CheckoutRequest request) {
        OrderResponse order = checkoutService.processCheckout(userId, request);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long orderId) {
        OrderResponse order = checkoutService.getOrder(orderId);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/orders/number/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrderByNumber(@PathVariable String orderNumber) {
        OrderResponse order = checkoutService.getOrderByNumber(orderNumber);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> getUserOrders(@RequestHeader("X-User-Id") String userId) {
        List<OrderResponse> orders = checkoutService.getUserOrders(userId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/orders/paged")
    public ResponseEntity<Page<OrderResponse>> getUserOrdersPaged(
            @RequestHeader("X-User-Id") String userId,
            Pageable pageable) {
        Page<OrderResponse> orders = checkoutService.getUserOrders(userId, pageable);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/orders/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status) {
        OrderResponse order = checkoutService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok(order);
    }
}