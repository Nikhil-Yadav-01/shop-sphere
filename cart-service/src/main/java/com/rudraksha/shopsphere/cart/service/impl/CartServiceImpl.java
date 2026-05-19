package com.rudraksha.shopsphere.cart.service.impl;

import com.rudraksha.shopsphere.cart.client.CatalogClient;
import com.rudraksha.shopsphere.cart.client.InventoryClient;
import com.rudraksha.shopsphere.cart.dto.request.AddToCartRequest;
import com.rudraksha.shopsphere.cart.dto.request.UpdateCartItemRequest;
import com.rudraksha.shopsphere.cart.dto.response.CartItemResponse;
import com.rudraksha.shopsphere.cart.dto.response.CartResponse;
import com.rudraksha.shopsphere.cart.dto.response.ProductResponse;
import com.rudraksha.shopsphere.cart.entity.Cart;
import com.rudraksha.shopsphere.cart.entity.CartItem;
import com.rudraksha.shopsphere.cart.exception.InsufficientStockException;
import com.rudraksha.shopsphere.cart.repository.CartRepository;
import com.rudraksha.shopsphere.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CatalogClient catalogClient;
    private final InventoryClient inventoryClient;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public CartResponse getCart(String userId) {
        Cart cart = getOrCreateCart(userId);
        refreshCartPrices(cart);
        return mapToResponse(cart);
    }

    @Override
    public CartResponse addToCart(String userId, AddToCartRequest request) {
        // 3.1 & 3.4: Fetch real product data with fallback
        ProductResponse product = catalogClient.getProductById(request.getProductId());
        
        // 3.2: Stock validation
        if (product.getSku() != null) {
            Boolean available = inventoryClient.checkAvailability(product.getSku(), request.getQuantity());
            if (Boolean.FALSE.equals(available)) {
                throw new InsufficientStockException("Insufficient stock for product: " + product.getName());
            }
        }

        Cart cart = getOrCreateCart(userId);

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            // Update price and name even if item already exists
            item.setPrice(product.getPrice());
            item.setProductName(product.getName());
        } else {
            CartItem newItem = CartItem.builder()
                    .productId(request.getProductId())
                    .productName(product.getName())
                    .quantity(request.getQuantity())
                    .price(product.getPrice())
                    .imageUrl(product.getImageUrl() != null ? product.getImageUrl() : "")
                    .build();
            cart.getItems().add(newItem);
        }

        cart.setUpdatedAt(LocalDateTime.now());
        refreshCartPrices(cart); // Refresh all items in case others changed
        saveCart(cart);

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

        // 3.2: Optional stock check on increase
        if (request.getQuantity() > item.getQuantity()) {
            ProductResponse product = catalogClient.getProductById(productId);
            if (product.getSku() != null) {
                Boolean available = inventoryClient.checkAvailability(product.getSku(), request.getQuantity());
                if (Boolean.FALSE.equals(available)) {
                    throw new InsufficientStockException("Insufficient stock for product: " + product.getName());
                }
            }
            // Update price and name while we're at it
            item.setPrice(product.getPrice());
            item.setProductName(product.getName());
        }

        item.setQuantity(request.getQuantity());
        cart.setUpdatedAt(LocalDateTime.now());
        refreshCartPrices(cart);
        saveCart(cart);

        log.info("Updated cart item {} for user {}", productId, userId);
        return mapToResponse(cart);
    }

    private void refreshCartPrices(Cart cart) {
        if (cart.getItems().isEmpty()) {
            return;
        }

        List<String> productIds = cart.getItems().stream()
                .map(CartItem::getProductId)
                .toList();

        try {
            List<ProductResponse> products = catalogClient.getProductsByIds(productIds);
            Map<String, ProductResponse> productMap = products.stream()
                    .collect(Collectors.toMap(ProductResponse::getId, p -> p));

            boolean changed = false;
            for (CartItem item : cart.getItems()) {
                ProductResponse product = productMap.get(item.getProductId());
                if (product != null) {
                    if (!product.getPrice().equals(item.getPrice())) {
                        item.setPrice(product.getPrice());
                        changed = true;
                    }
                    if (!product.getName().equals(item.getProductName())) {
                        item.setProductName(product.getName());
                        changed = true;
                    }
                }
            }
            if (changed) {
                saveCart(cart);
                log.info("Refreshed prices for cart of user {}", cart.getUserId());
            }
        } catch (Exception e) {
            log.warn("Failed to refresh cart prices for user {}: {}", cart.getUserId(), e.getMessage());
        }
    }

    @Override
    public CartResponse removeFromCart(String userId, String productId) {
        Cart cart = getOrCreateCart(userId);

        boolean removed = cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        if (!removed) {
            throw new IllegalArgumentException("Product not found in cart");
        }

        cart.setUpdatedAt(LocalDateTime.now());
        saveCart(cart);

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

    private void saveCart(Cart cart) {
        cartRepository.save(cart);
        // 3.3: Explicit TTL enforcement
        redisTemplate.expire("cart:" + cart.getUserId(), Duration.ofDays(7));
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
                    saveCart(newCart);
                    return newCart;
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
