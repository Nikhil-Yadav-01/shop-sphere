package com.rudraksha.shopsphere.analytics.controller;

import com.rudraksha.shopsphere.analytics.dto.AnalyticsResponse;
import com.rudraksha.shopsphere.analytics.dto.ApiResponse;
import com.rudraksha.shopsphere.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @PostMapping("/events")
    public ResponseEntity<ApiResponse<String>> ingestEvent(
            @RequestParam String eventType,
            @RequestParam Long userId,
            @RequestParam String sessionId,
            @RequestBody Map<String, Object> eventData,
            @RequestHeader(value = "X-Forwarded-For", required = false) String ipAddress,
            @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        log.info("Ingest event: {}", eventType);
        analyticsService.ingestEvent(eventType, userId, sessionId, eventData, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success("Event ingested"));
    }

    @GetMapping("/events/{eventType}")
    public ResponseEntity<ApiResponse<List<AnalyticsResponse>>> getEventsByType(@PathVariable String eventType) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getEventsByType(eventType)));
    }

    @GetMapping("/users/{userId}/events")
    public ResponseEntity<ApiResponse<List<AnalyticsResponse>>> getUserEvents(
            @PathVariable Long userId,
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getUserEvents(userId, startDate, endDate)));
    }

    @GetMapping("/events/recent")
    public ResponseEntity<ApiResponse<List<AnalyticsResponse>>> getRecentEvents(
            @RequestParam String eventType,
            @RequestParam LocalDateTime since) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getRecentEvents(eventType, since)));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getTotalEventCount() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getTotalEventCount()));
    }

    @GetMapping("/count/{eventType}")
    public ResponseEntity<ApiResponse<Long>> getEventCountByType(@PathVariable String eventType) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getEventCountByType(eventType)));
    }

    @PostMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Analytics Service is running"));
    }
}
