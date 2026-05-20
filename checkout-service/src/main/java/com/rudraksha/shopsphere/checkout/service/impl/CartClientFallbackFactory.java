package com.rudraksha.shopsphere.checkout.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;

@Component
@Slf4j
public class CartClientFallbackFactory implements FallbackFactory<CheckoutServiceImpl.CartClient> {
    @Override
    public CheckoutServiceImpl.CartClient create(Throwable cause) {
        return new CheckoutServiceImpl.CartClient() {
            @Override
            public CartResponse getCart(String userId) {
                log.error("Fallback triggered for getCart for User ID: {} due to: {}", userId, cause.getMessage(), cause);
                return new CartResponse(null, userId, Collections.emptyList(), 0, BigDecimal.ZERO);
            }

            @Override
            public void clearCart(String userId) {
                log.error("Fallback triggered for clearCart for User ID: {} due to: {}", userId, cause.getMessage(), cause);
            }
        };
    }
}
