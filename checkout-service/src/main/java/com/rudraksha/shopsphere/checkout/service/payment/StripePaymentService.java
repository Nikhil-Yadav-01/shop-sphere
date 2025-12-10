package com.rudraksha.shopsphere.checkout.service.payment;

import com.rudraksha.shopsphere.checkout.dto.request.PaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@ConditionalOnProperty(name = "payment.gateway.provider", havingValue = "stripe")
@Slf4j
public class StripePaymentService implements PaymentService {

    @Value("${payment.gateway.api-key}")
    private String apiKey;

    @Override
    public PaymentResult processPayment(PaymentRequest paymentRequest, BigDecimal amount, String orderNumber) {
        log.info("Processing Stripe payment for order: {} with amount: {}", orderNumber, amount);
        
        // TODO: Implement actual Stripe integration when API key is available
        // Stripe.apiKey = apiKey;
        // PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
        //     .setAmount(amount.multiply(BigDecimal.valueOf(100)).longValue()) // Convert to cents
        //     .setCurrency("usd")
        //     .setDescription("Order: " + orderNumber)
        //     .build();
        // PaymentIntent intent = PaymentIntent.create(params);
        
        throw new UnsupportedOperationException("Stripe integration requires valid API key. Currently using mock payment service.");
    }
}