package com.rudraksha.shopsphere.checkout.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * @deprecated Replaced by {@link PaymentEventConsumer} and {@link InventoryEventConsumer}.
 * This class is kept as a placeholder to avoid build breaks on any external references,
 * but contains no active Kafka listener logic.
 */
@Deprecated(since = "2.0", forRemoval = true)
@Slf4j
@Component
public class PaymentEventListener {
    // Superseded by PaymentEventConsumer and InventoryEventConsumer
    // which use structured EventEnvelope<T> deserialization and Redis idempotency.
}
