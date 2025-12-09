package com.rudraksha.shopsphere.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsResponse {
    private String id;
    private String eventType;
    private Long userId;
    private String sessionId;
    private Map<String, Object> eventData;
    private String ipAddress;
    private LocalDateTime timestamp;
}
