package com.rudraksha.shopsphere.analytics.kafka;

import com.rudraksha.shopsphere.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {

    private final AnalyticsService analyticsService;

    @KafkaListener(topics = "user-events", groupId = "analytics-service")
    public void consumeUserEvent(Map<String, Object> event) {
        try {
            log.info("Consumed user event: {}", event);
            String eventType = (String) event.get("eventType");
            Object userIdObj = event.get("userId");
            Long userId = userIdObj instanceof Number ? ((Number) userIdObj).longValue() : Long.parseLong(userIdObj.toString());
            String sessionId = (String) event.getOrDefault("sessionId", "kafka-session");
            
            analyticsService.ingestEvent(eventType, userId, sessionId, event, null, null);
        } catch (Exception e) {
            log.error("Error consuming user event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "order-events", groupId = "analytics-service")
    public void consumeOrderEvent(Map<String, Object> event) {
        try {
            log.info("Consumed order event: {}", event);
            String action = (String) event.getOrDefault("action", "PLACED");
            String eventType = "ORDER_" + action;
            Object userIdObj = event.get("userId");
            Long userId = userIdObj instanceof Number ? ((Number) userIdObj).longValue() : Long.parseLong(userIdObj.toString());
            
            analyticsService.ingestEvent(eventType, userId, "order-session", event, null, null);
        } catch (Exception e) {
            log.error("Error consuming order event: {}", e.getMessage());
        }
    }
}
