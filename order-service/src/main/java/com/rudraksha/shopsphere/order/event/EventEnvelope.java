package com.rudraksha.shopsphere.order.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventEnvelope<T> {
    @Builder.Default
    private String id = UUID.randomUUID().toString(); // idempotency key
    private String type;
    private String source;
    @Builder.Default
    private String schemaVersion = "1.0";
    @Builder.Default
    private Instant timestamp = Instant.now();
    private T payload;
}
