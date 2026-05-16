package com.rudraksha.shopsphere.payment.kafka;

import com.rudraksha.shopsphere.payment.dto.request.ProcessPaymentRequest;
import com.rudraksha.shopsphere.payment.service.PaymentService;
import com.rudraksha.shopsphere.payment.entity.Payment;
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
public class InventoryEventListener {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "inventory.reserved", groupId = "payment-service-group")
    public void onInventoryReserved(String message) {
        log.info("Received inventory.reserved event in payment-service: {}", message);
        try {
            JsonNode node = objectMapper.readTree(message);
            if (node.has("orderNumber") && node.has("userId") && !node.get("userId").asText().isEmpty()) {
                String orderNumber = node.get("orderNumber").asText();
                String userId = node.get("userId").asText();
                BigDecimal amount = new BigDecimal(node.get("totalAmount").asText());
                
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    log.warn("Skipping payment for order {} with zero amount", orderNumber);
                    return;
                }

                ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                    .orderNumber(orderNumber)
                    .userId(userId)
                    .amount(amount)
                    .currency("USD")
                    .method(Payment.PaymentMethod.CREDIT_CARD)
                    .paymentMethodDetails("Mock payment details")
                    .build();
                
                paymentService.processPayment(request);
            }
        } catch (Exception e) {
            log.error("Failed to process inventory.reserved event in payment-service", e);
        }
    }
}
