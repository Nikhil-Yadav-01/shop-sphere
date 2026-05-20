package com.rudraksha.shopsphere.notification.service;

import com.rudraksha.shopsphere.notification.dto.NotificationRequest;
import com.rudraksha.shopsphere.notification.dto.NotificationResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface NotificationService {

    NotificationResponse createNotification(NotificationRequest request);

    NotificationResponse getNotificationById(Long id);

    Page<NotificationResponse> getNotificationsByUserId(String userId, Pageable pageable);

    Page<NotificationResponse> getUnreadNotificationsByUserId(String userId, Pageable pageable);

    NotificationResponse markAsRead(Long notificationId);

    List<NotificationResponse> markAllAsRead(String userId);

    void deleteNotification(Long notificationId);

    long getUnreadCount(String userId);

    Page<NotificationResponse> getRecentNotifications(String userId, int days, Pageable pageable);

    void registerPushToken(String userId, String token, String deviceType);
}
