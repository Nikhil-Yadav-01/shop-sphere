package com.rudraksha.shopsphere.checkout.service.impl;

import com.rudraksha.shopsphere.checkout.client.CartClient;
import com.rudraksha.shopsphere.checkout.exception.CheckoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * @deprecated Use {@link com.rudraksha.shopsphere.checkout.client.fallback.CartClientFallbackFactory} instead.
 * Kept for backward compatibility during refactor. Will be removed once callers are updated.
 */
@Deprecated(since = "2.0", forRemoval = true)
@Component("legacyCartClientFallbackFactory")
@Slf4j
public class CartClientFallbackFactory implements FallbackFactory<CartClient> {

    @Override
    public CartClient create(Throwable cause) {
        return new CartClient() {
            @Override
            public CartResponse getCart(String userId) {
                log.error("CartClient.getCart fallback triggered for userId={}: {}", userId, cause.getMessage());
                throw new CheckoutException("Cart service is currently unavailable. Please try again later.",
                        HttpStatus.SERVICE_UNAVAILABLE, cause);
            }

            @Override
            public void clearCart(String userId) {
                log.error("CartClient.clearCart fallback triggered for userId={}: {}", userId, cause.getMessage());
            }
        };
    }
}
