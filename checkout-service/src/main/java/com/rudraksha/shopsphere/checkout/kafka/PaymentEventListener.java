package com.rudraksha.shopsphere.checkout.kafka;

import com.rudraksha.shopsphere.checkout.entity.Order;
import com.rudraksha.shopsphere.checkout.repository.OrderRepository;
import com.rudraksha.shopsphere.checkout.service.impl.CheckoutServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final OrderRepository orderRepository;
    private final CheckoutServiceImpl.CartClient cartClient;

    @KafkaListener(topics = "payment-events", groupId = "checkout-service-group")
    public void onPaymentEvent(String message) {
        log.info("Received payment event: {}", message);
        try {
            String[] parts = message.split(":");
            if (parts.length >= 3) {
                String eventType = parts[0];
                String transactionId = parts[1];
                String orderNumber = parts[2]; // assuming payment service will send string order number

                orderRepository.findByOrderNumber(orderNumber).ifPresent(order -> {
                    if ("PAYMENT_PROCESSED".equals(eventType) || "PAYMENT_SUCCESS".equals(eventType)) {
                        order.setStatus(Order.OrderStatus.CONFIRMED);
                        order.setTransactionId(transactionId);
                        orderRepository.save(order);
                        log.info("Order {} confirmed. Transaction ID: {}", orderNumber, transactionId);
                        
                        try {
                            cartClient.clearCart(order.getUserId());
                        } catch (Exception e) {
                            log.error("Failed to clear cart for user {}", order.getUserId(), e);
                        }
                    } else if ("PAYMENT_FAILED".equals(eventType)) {
                        order.setStatus(Order.OrderStatus.CANCELLED);
                        order.setTransactionId(transactionId);
                        orderRepository.save(order);
                        log.warn("Order {} cancelled due to payment failure.", orderNumber);
                    }
                });
            }
        } catch (Exception e) {
            log.error("Error processing payment event", e);
        }
    }
}