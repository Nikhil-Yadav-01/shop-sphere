package com.rudraksha.shopsphere.analytics.service;

import com.rudraksha.shopsphere.analytics.document.AnalyticsEvent;
import com.rudraksha.shopsphere.analytics.dto.AnalyticsResponse;
import com.rudraksha.shopsphere.analytics.repository.AnalyticsEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    public void ingestEvent(String eventType, Long userId, String sessionId, Map<String, Object> eventData, String ipAddress, String userAgent) {
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
    public List<AnalyticsResponse> getEventsByType(String eventType) {
        return repository.findByEventType(eventType).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AnalyticsResponse> getUserEvents(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return repository.findByUserIdAndTimestampBetween(userId, startDate, endDate).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AnalyticsResponse> getRecentEvents(String eventType, LocalDateTime since) {
        return repository.findByEventTypeAndTimestampAfter(eventType, since).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
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
