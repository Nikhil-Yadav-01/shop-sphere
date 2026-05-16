package com.rudraksha.shopsphere.payment.kafka;

import com.rudraksha.shopsphere.payment.dto.request.ProcessPaymentRequest;
import com.rudraksha.shopsphere.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class CheckoutEventListener {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "checkout.initiated", groupId = "payment-service-group")
    public void onCheckoutInitiated(String message) {
        log.info("Received checkout.initiated event: {}", message);
        try {
            JsonNode node = objectMapper.readTree(message);
            String orderNumber = node.get("orderNumber").asText();
            BigDecimal amount = new BigDecimal(node.get("totalAmount").asText());
            String userId = node.get("userId").asText();
            
            // Assume method is CREDIT_CARD for mock
            ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .orderNumber(orderNumber)
                .customerId(userId) // Using the string userId from checkout
                .amount(amount)
                .currency("USD")
                .method(com.rudraksha.shopsphere.payment.entity.Payment.PaymentMethod.CREDIT_CARD)
                .paymentMethodDetails("Mock payment details")
                .build();
            
            // Call the existing logic which publishes PAYMENT_PROCESSED
            paymentService.processPayment(request);
        } catch (Exception e) {
            log.error("Failed to process checkout.initiated event", e);
        }
    }
}