package com.rudraksha.shopsphere.checkout.client;

import com.rudraksha.shopsphere.checkout.client.fallback.CartClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

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
        BigDecimal totalPrice
    ) {}

    record CartItemResponse(
        String productId,
        String productName,
        Integer quantity,
        BigDecimal price,
        BigDecimal subtotal
    ) {}
}
