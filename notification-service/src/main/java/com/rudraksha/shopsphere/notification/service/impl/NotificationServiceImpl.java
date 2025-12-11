package com.rudraksha.shopsphere.notification.service.impl;

import com.rudraksha.shopsphere.notification.dto.NotificationRequest;
import com.rudraksha.shopsphere.notification.dto.NotificationResponse;
import com.rudraksha.shopsphere.notification.entity.Notification;
import com.rudraksha.shopsphere.notification.exception.NotificationException;
import com.rudraksha.shopsphere.notification.repository.NotificationRepository;
import com.rudraksha.shopsphere.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public NotificationResponse createNotification(NotificationRequest request) {
        try {
            Notification notification = new Notification();
            notification.setUserId(request.getUserId());
            notification.setType(request.getType());
            notification.setTitle(request.getTitle());
            notification.setMessage(request.getMessage());
            notification.setChannel(request.getChannel());
            notification.setRecipientEmail(request.getRecipientEmail());
            notification.setRecipientPhone(request.getRecipientPhone());
            notification.setStatus(Notification.NotificationStatus.PENDING);
            notification.setIsRead(false);

            Notification saved = notificationRepository.save(notification);
            log.info("Notification created with ID: {}", saved.getId());
            return NotificationResponse.fromEntity(saved);
        } catch (Exception e) {
            log.error("Error creating notification", e);
            throw new NotificationException("Failed to create notification: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(Long id) {
        return notificationRepository.findById(id)
                .map(NotificationResponse::fromEntity)
                .orElseThrow(() -> new NotificationException("Notification not found with ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByUserId(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotificationsByUserId(String userId) {
        return notificationRepository.findByUserIdAndIsRead(userId, false)
                .stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }

    @Override
    public NotificationResponse markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationException("Notification not found with ID: " + notificationId));

        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        notification.setStatus(Notification.NotificationStatus.DELIVERED);

        Notification updated = notificationRepository.save(notification);
        log.info("Notification marked as read: {}", notificationId);
        return NotificationResponse.fromEntity(updated);
    }

    @Override
    public List<NotificationResponse> markAllAsRead(String userId) {
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsRead(userId, false);
        LocalDateTime now = LocalDateTime.now();

        unreadNotifications.forEach(notification -> {
            notification.setIsRead(true);
            notification.setReadAt(now);
            notification.setStatus(Notification.NotificationStatus.DELIVERED);
        });

        notificationRepository.saveAll(unreadNotifications);
        log.info("Marked {} notifications as read for user: {}", unreadNotifications.size(), userId);

        return unreadNotifications.stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }

    @Override
    public void deleteNotification(Long notificationId) {
        notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationException("Notification not found with ID: " + notificationId));
        notificationRepository.deleteById(notificationId);
        log.info("Notification deleted: {}", notificationId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getRecentNotifications(String userId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return notificationRepository.findRecentNotifications(userId, since)
                .stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }
}
