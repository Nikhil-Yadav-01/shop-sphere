package com.rudraksha.shopsphere.order.kafka;

import com.rudraksha.shopsphere.order.entity.Order;
import com.rudraksha.shopsphere.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final OrderRepository orderRepository;

    @KafkaListener(topics = "payment-events", groupId = "order-service-group")
    public void onPaymentEvent(String message) {
        log.info("Received payment event: {}", message);
        try {
            String[] parts = message.split(":");
            if (parts.length >= 3) {
                String eventType = parts[0];
                String transactionId = parts[1];
                String orderNumber = parts[2];

                orderRepository.findByOrderNumber(orderNumber).ifPresent(order -> {
                    if ("PAYMENT_SUCCESS".equals(eventType) || "PAYMENT_PROCESSED".equals(eventType)) {
                        order.setStatus(Order.OrderStatus.CONFIRMED);
                        orderRepository.save(order);
                        log.info("Order {} confirmed via payment event.", orderNumber);
                    } else if ("PAYMENT_FAILED".equals(eventType)) {
                        order.setStatus(Order.OrderStatus.CANCELLED);
                        orderRepository.save(order);
                        log.warn("Order {} cancelled via payment event.", orderNumber);
                    }
                });
            }
        } catch (Exception e) {
            log.error("Error processing payment event", e);
        }
    }
}
