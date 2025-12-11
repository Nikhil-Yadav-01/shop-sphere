package com.rudraksha.shopsphere.notification.service;

import com.rudraksha.shopsphere.notification.dto.NotificationRequest;
import com.rudraksha.shopsphere.notification.dto.NotificationResponse;

import java.util.List;

public interface NotificationService {

    NotificationResponse createNotification(NotificationRequest request);

    NotificationResponse getNotificationById(Long id);

    List<NotificationResponse> getNotificationsByUserId(String userId);

    List<NotificationResponse> getUnreadNotificationsByUserId(String userId);

    NotificationResponse markAsRead(Long notificationId);

    List<NotificationResponse> markAllAsRead(String userId);

    void deleteNotification(Long notificationId);

    long getUnreadCount(String userId);

    List<NotificationResponse> getRecentNotifications(String userId, int days);
}
