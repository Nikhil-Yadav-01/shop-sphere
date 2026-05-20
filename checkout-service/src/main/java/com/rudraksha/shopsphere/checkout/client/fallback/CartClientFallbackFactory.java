package com.rudraksha.shopsphere.checkout.client.fallback;

import com.rudraksha.shopsphere.checkout.client.CartClient;
import com.rudraksha.shopsphere.checkout.exception.CheckoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
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
                // Best-effort: log but don't fail the checkout — cart cleared on next successful call
                log.error("CartClient.clearCart fallback triggered for userId={}: {}", userId, cause.getMessage());
            }
        };
    }
}
