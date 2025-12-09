package com.rudraksha.shopsphere.cart.service.impl;

import com.rudraksha.shopsphere.cart.dto.request.AddToCartRequest;
import com.rudraksha.shopsphere.cart.dto.request.UpdateCartItemRequest;
import com.rudraksha.shopsphere.cart.dto.response.CartItemResponse;
import com.rudraksha.shopsphere.cart.dto.response.CartResponse;
import com.rudraksha.shopsphere.cart.entity.Cart;
import com.rudraksha.shopsphere.cart.entity.CartItem;
import com.rudraksha.shopsphere.cart.repository.CartRepository;
import com.rudraksha.shopsphere.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;

    @Override
    public CartResponse getCart(String userId) {
        Cart cart = getOrCreateCart(userId);
        return mapToResponse(cart);
    }

    @Override
    public CartResponse addToCart(String userId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(userId);

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
        } else {
            CartItem newItem = CartItem.builder()
                    .productId(request.getProductId())
                    .productName("Product " + request.getProductId())
                    .quantity(request.getQuantity())
                    .price(BigDecimal.valueOf(99.99))
                    .imageUrl("")
                    .build();
            cart.getItems().add(newItem);
        }

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        log.info("Added product {} to cart for user {}", request.getProductId(), userId);
        return mapToResponse(cart);
    }

    @Override
    public CartResponse updateCartItem(String userId, String productId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(userId);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Product not found in cart"));

        item.setQuantity(request.getQuantity());
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        log.info("Updated cart item {} for user {}", productId, userId);
        return mapToResponse(cart);
    }

    @Override
    public CartResponse removeFromCart(String userId, String productId) {
        Cart cart = getOrCreateCart(userId);

        boolean removed = cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        if (!removed) {
            throw new IllegalArgumentException("Product not found in cart");
        }

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        log.info("Removed product {} from cart for user {}", productId, userId);
        return mapToResponse(cart);
    }

    @Override
    public void clearCart(String userId) {
        cartRepository.findById(userId).ifPresent(cart -> {
            cartRepository.delete(cart);
            log.info("Cleared cart for user {}", userId);
        });
    }

    private Cart getOrCreateCart(String userId) {
        return cartRepository.findById(userId)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .userId(userId)
                            .id(UUID.randomUUID().toString())
                            .items(new ArrayList<>())
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .expiresAt(LocalDateTime.now().plusDays(7))
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private CartResponse mapToResponse(Cart cart) {
        var itemResponses = cart.getItems().stream()
                .map(this::mapItemToResponse)
                .toList();

        BigDecimal totalPrice = itemResponses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = itemResponses.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(itemResponses)
                .totalItems(totalItems)
                .totalPrice(totalPrice)
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    private CartItemResponse mapItemToResponse(CartItem item) {
        BigDecimal subtotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return CartItemResponse.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .subtotal(subtotal)
                .imageUrl(item.getImageUrl())
                .build();
    }
}
