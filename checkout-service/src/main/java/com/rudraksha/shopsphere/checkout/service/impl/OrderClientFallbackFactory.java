package com.rudraksha.shopsphere.checkout.service.impl;

import com.rudraksha.shopsphere.checkout.client.CartClient;
import com.rudraksha.shopsphere.checkout.exception.CheckoutException;
import com.rudraksha.shopsphere.checkout.exception.OrderServiceUnavailableException;
import com.rudraksha.shopsphere.checkout.dto.response.OrderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * @deprecated Legacy fallback for the old inline OrderClient.
 * OrderClient has been removed in the SAGA refactor. Kept to avoid compilation failures
 * during the transition period. Will be deleted once the branch is stable.
 */
@Deprecated(since = "2.0", forRemoval = true)
@Slf4j
@Component("legacyOrderClientFallbackFactory")
public class OrderClientFallbackFactory implements FallbackFactory<CartClient> {

    @Override
    public CartClient create(Throwable cause) {
        return new CartClient() {
            @Override
            public CartClient.CartResponse getCart(String userId) {
                log.error("Order Service fallback triggered for getCart due to: {}", cause.getMessage(), cause);
                throw new CheckoutException("Order Service is currently unavailable.",
                        HttpStatus.SERVICE_UNAVAILABLE, cause);
            }

            @Override
            public void clearCart(String userId) {
                log.error("Order Service fallback triggered for clearCart due to: {}", cause.getMessage(), cause);
            }
        };
    }
}
