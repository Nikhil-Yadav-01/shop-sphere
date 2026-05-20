package com.rudraksha.shopsphere.analytics.service;

import com.rudraksha.shopsphere.analytics.dto.AnalyticsResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AnalyticsService {
    void ingestEvent(String eventType, String userId, String sessionId, Map<String, Object> eventData, String ipAddress, String userAgent);
    Page<AnalyticsResponse> getEventsByType(String eventType, Pageable pageable);
    Page<AnalyticsResponse> getUserEvents(String userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    Page<AnalyticsResponse> getRecentEvents(String eventType, LocalDateTime since, Pageable pageable);
    long getTotalEventCount();
    long getEventCountByType(String eventType);
    AnalyticsResponse getEventById(String eventId);
}
