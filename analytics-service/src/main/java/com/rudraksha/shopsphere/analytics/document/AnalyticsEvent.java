package com.rudraksha.shopsphere.analytics.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "analytics_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsEvent {

    @MongoId
    private String id;
    private String eventType;
    private Long userId;
    private String sessionId;
    private Map<String, Object> eventData;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime timestamp;
    
    @Builder.Default
    private boolean processed = false;
}
