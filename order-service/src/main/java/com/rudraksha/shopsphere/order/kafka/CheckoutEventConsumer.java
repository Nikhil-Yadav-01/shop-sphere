package com.rudraksha.shopsphere.order.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rudraksha.shopsphere.order.dto.request.CreateOrderRequest;
import com.rudraksha.shopsphere.order.dto.request.OrderItemRequest;
import com.rudraksha.shopsphere.order.event.CheckoutInitiatedPayload;
import com.rudraksha.shopsphere.order.event.EventEnvelope;
import com.rudraksha.shopsphere.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CheckoutEventConsumer {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String IDEMPOTENCY_KEY_PREFIX = "order:event:processed:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    @KafkaListener(
            topics = "checkout.initiated",
            groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onCheckoutInitiated(String message, Acknowledgment ack) {
        log.info("Received checkout.initiated event in order-service: {}", message);
        try {
            EventEnvelope<CheckoutInitiatedPayload> envelope = objectMapper.readValue(
                    message,
                    new TypeReference<EventEnvelope<CheckoutInitiatedPayload>>() {}
            );

            String eventId = envelope.getId();
            String idempotencyKey = IDEMPOTENCY_KEY_PREFIX + eventId;

            // Consumer Idempotency Check using Redis
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(idempotencyKey, "PROCESSED", IDEMPOTENCY_TTL);
            if (Boolean.FALSE.equals(isNew)) {
                log.warn("Event {} already processed, skipping duplicate checkout.initiated event.", eventId);
                ack.acknowledge();
                return;
            }

            CheckoutInitiatedPayload payload = envelope.getPayload();
            if (payload == null) {
                log.error("Event {} has null payload, ignoring.", eventId);
                ack.acknowledge();
                return;
            }

            log.info("Creating pending order for checkout session: {}", payload.getSessionId());

            List<OrderItemRequest> itemRequests = payload.getItems().stream()
                    .map(item -> OrderItemRequest.builder()
                            .productId(item.getProductId())
                            .productName(item.getProductName())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .totalPrice(item.getTotalPrice())
                            .build())
                    .collect(Collectors.toList());

            CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                    .userId(payload.getUserId())
                    .totalAmount(payload.getTotalAmount())
                    .taxAmount(payload.getTaxAmount())
                    .shippingAddress(payload.getShippingAddress())
                    .billingAddress(payload.getBillingAddress())
                    .checkoutSessionId(payload.getSessionId())
                    .items(itemRequests)
                    .build();

            orderService.createOrder(orderRequest);
            log.info("Pending order successfully created for session: {}", payload.getSessionId());
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process checkout.initiated event: {}", message, e);
            // In Choreographed SAGA, if order creation fails, checkout session will eventually time out or we can publish order failed.
            // But we don't acknowledge so it can be retried if it was a transient error (e.g. DB connection issues).
        }
    }
}
