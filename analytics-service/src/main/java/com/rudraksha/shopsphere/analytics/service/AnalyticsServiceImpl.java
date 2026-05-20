package com.rudraksha.shopsphere.analytics.service;

import com.rudraksha.shopsphere.analytics.document.AnalyticsEvent;
import com.rudraksha.shopsphere.analytics.dto.AnalyticsResponse;
import com.rudraksha.shopsphere.analytics.repository.AnalyticsEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    private final AnalyticsEventRepository repository;

    @Override
    public void ingestEvent(String eventType, String userId, String sessionId, Map<String, Object> eventData, String ipAddress, String userAgent) {
        AnalyticsEvent event = AnalyticsEvent.builder()
                .eventType(eventType)
                .userId(userId)
                .sessionId(sessionId)
                .eventData(eventData)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .timestamp(LocalDateTime.now())
                .build();
        repository.save(event);
        log.info("Event ingested: {}", eventType);
    }

    @Override
    public Page<AnalyticsResponse> getEventsByType(String eventType, Pageable pageable) {
        return repository.findByEventType(eventType, pageable)
                .map(this::toResponse);
    }

    @Override
    public Page<AnalyticsResponse> getUserEvents(String userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return repository.findByUserIdAndTimestampBetween(userId, startDate, endDate, pageable)
                .map(this::toResponse);
    }

    @Override
    public Page<AnalyticsResponse> getRecentEvents(String eventType, LocalDateTime since, Pageable pageable) {
        return repository.findByEventTypeAndTimestampAfter(eventType, since, pageable)
                .map(this::toResponse);
    }

    @Override
    public long getTotalEventCount() {
        return repository.count();
    }

    @Override
    public long getEventCountByType(String eventType) {
        return repository.countByEventType(eventType);
    }

    @Override
    public AnalyticsResponse getEventById(String eventId) {
        return repository.findById(eventId)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Event not found"));
    }

    private AnalyticsResponse toResponse(AnalyticsEvent event) {
        return AnalyticsResponse.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .userId(event.getUserId())
                .sessionId(event.getSessionId())
                .eventData(event.getEventData())
                .ipAddress(event.getIpAddress())
                .timestamp(event.getTimestamp())
                .build();
    }
}
