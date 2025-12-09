package com.rudraksha.shopsphere.analytics.service;

import com.rudraksha.shopsphere.analytics.dto.AnalyticsResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AnalyticsService {
    void ingestEvent(String eventType, Long userId, String sessionId, Map<String, Object> eventData, String ipAddress, String userAgent);
    List<AnalyticsResponse> getEventsByType(String eventType);
    List<AnalyticsResponse> getUserEvents(Long userId, LocalDateTime startDate, LocalDateTime endDate);
    List<AnalyticsResponse> getRecentEvents(String eventType, LocalDateTime since);
    long getTotalEventCount();
    long getEventCountByType(String eventType);
    AnalyticsResponse getEventById(String eventId);
}
