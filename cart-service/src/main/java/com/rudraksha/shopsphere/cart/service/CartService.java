package com.rudraksha.shopsphere.cart.service;

import com.rudraksha.shopsphere.cart.dto.request.AddToCartRequest;
import com.rudraksha.shopsphere.cart.dto.request.UpdateCartItemRequest;
import com.rudraksha.shopsphere.cart.dto.response.CartResponse;

public interface CartService {
    CartResponse getCart(String userId);
    CartResponse addToCart(String userId, AddToCartRequest request);
    CartResponse updateCartItem(String userId, String productId, UpdateCartItemRequest request);
    CartResponse removeFromCart(String userId, String productId);
    void clearCart(String userId);
}
