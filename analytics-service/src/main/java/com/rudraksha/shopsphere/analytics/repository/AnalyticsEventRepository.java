package com.rudraksha.shopsphere.analytics.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.rudraksha.shopsphere.analytics.document.AnalyticsEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AnalyticsEventRepository extends MongoRepository<AnalyticsEvent, String> {
    Page<AnalyticsEvent> findByEventType(String eventType, Pageable pageable);
    Page<AnalyticsEvent> findByUserIdAndTimestampBetween(String userId, LocalDateTime start, LocalDateTime end, Pageable pageable);
    Page<AnalyticsEvent> findByEventTypeAndTimestampAfter(String eventType, LocalDateTime since, Pageable pageable);
    long countByEventType(String eventType);
}
