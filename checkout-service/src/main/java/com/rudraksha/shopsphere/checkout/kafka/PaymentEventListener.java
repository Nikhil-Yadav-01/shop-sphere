package com.rudraksha.shopsphere.checkout.kafka;

import com.rudraksha.shopsphere.checkout.service.impl.CheckoutServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final CheckoutServiceImpl.CartClient cartClient;

    @KafkaListener(topics = "payment-events", groupId = "checkout-service-group")
    public void onPaymentEvent(String message) {
        log.info("Received payment event in checkout-service: {}", message);
        try {
            String[] parts = message.split(":");
            if (parts.length >= 4) {
                String eventType = parts[0];
                String orderNumber = parts[2];
                String userId = parts[3];

                if ("PAYMENT_SUCCESS".equals(eventType) || "PAYMENT_PROCESSED".equals(eventType)) {
                    log.info("Payment successful for order {}. Clearing cart for user {}.", orderNumber, userId);
                    try {
                        cartClient.clearCart(userId);
                        log.info("Cart cleared for user {}.", userId);
                    } catch (Exception e) {
                        log.error("Failed to clear cart for user {} after successful payment.", userId, e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error in checkout-service payment listener", e);
        }
    }
}
