package com.rudraksha.shopsphere.shared.models.events;

import com.rudraksha.shopsphere.shared.models.enums.EventType;
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
public class EventEnvelope<T> {

    @Builder.Default
    private UUID eventId = UUID.randomUUID();

    private EventType eventType;

    @Builder.Default
    private Instant timestamp = Instant.now();

    private T payload;

    @Builder.Default
    private SchemaVersion schemaVersion = SchemaVersion.V1;

    public static <T> EventEnvelope<T> of(EventType eventType, T payload) {
        return EventEnvelope.<T>builder()
                .eventType(eventType)
                .payload(payload)
                .build();
    }
}
