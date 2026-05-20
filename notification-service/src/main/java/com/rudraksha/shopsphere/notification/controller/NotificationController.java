package com.rudraksha.shopsphere.notification.controller;

import com.rudraksha.shopsphere.notification.dto.NotificationRequest;
import com.rudraksha.shopsphere.notification.dto.NotificationResponse;
import com.rudraksha.shopsphere.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<NotificationResponse> createNotification(
            @Valid @RequestBody NotificationRequest request) {
        NotificationResponse response = notificationService.createNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getNotification(@PathVariable Long id) {
        NotificationResponse response = notificationService.getNotificationById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<NotificationResponse>> getUserNotifications(@PathVariable String userId, Pageable pageable) {
        return ResponseEntity.ok(notificationService.getNotificationsByUserId(userId, pageable));
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<Page<NotificationResponse>> getUnreadNotifications(@PathVariable String userId, Pageable pageable) {
        return ResponseEntity.ok(notificationService.getUnreadNotificationsByUserId(userId, pageable));
    }

    @GetMapping("/user/{userId}/unread/count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(@PathVariable String userId) {
        long count = notificationService.getUnreadCount(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("unreadCount", count);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id) {
        NotificationResponse response = notificationService.markAsRead(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<List<NotificationResponse>> markAllAsRead(@PathVariable String userId) {
        List<NotificationResponse> response = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Notification deleted successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}/recent")
    public ResponseEntity<Page<NotificationResponse>> getRecentNotifications(
            @PathVariable String userId,
            @RequestParam(defaultValue = "7") int days,
            Pageable pageable) {
        Page<NotificationResponse> notifications = notificationService.getRecentNotifications(userId, days, pageable);
        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/tokens")
    public ResponseEntity<Void> registerToken(
            @RequestParam String userId,
            @RequestParam String token,
            @RequestParam(required = false) String deviceType) {
        notificationService.registerPushToken(userId, token, deviceType);
        return ResponseEntity.ok().build();
    }
}
