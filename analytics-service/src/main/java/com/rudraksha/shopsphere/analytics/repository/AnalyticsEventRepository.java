package com.rudraksha.shopsphere.analytics.repository;

import com.rudraksha.shopsphere.analytics.document.AnalyticsEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AnalyticsEventRepository extends MongoRepository<AnalyticsEvent, String> {
    List<AnalyticsEvent> findByEventType(String eventType);
    List<AnalyticsEvent> findByUserIdAndTimestampBetween(Long userId, LocalDateTime start, LocalDateTime end);
    List<AnalyticsEvent> findByEventTypeAndTimestampAfter(String eventType, LocalDateTime since);
    long countByEventType(String eventType);
}
