package com.rudraksha.shopsphere.notification.service.impl;

import com.rudraksha.shopsphere.notification.dto.NotificationRequest;
import com.rudraksha.shopsphere.notification.dto.NotificationResponse;
import com.rudraksha.shopsphere.notification.entity.Notification;
import com.rudraksha.shopsphere.notification.entity.UserPushToken;
import com.rudraksha.shopsphere.notification.exception.NotificationException;
import com.rudraksha.shopsphere.notification.repository.NotificationRepository;
import com.rudraksha.shopsphere.notification.repository.UserPushTokenRepository;
import com.rudraksha.shopsphere.notification.service.EmailService;
import com.rudraksha.shopsphere.notification.service.NotificationService;
import com.rudraksha.shopsphere.notification.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserPushTokenRepository pushTokenRepository;
    private final EmailService emailService;
    private final PushNotificationService pushNotificationService;

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
            
            // Trigger actual delivery
            sendNotification(saved);
            
            return NotificationResponse.fromEntity(saved);
        } catch (Exception e) {
            log.error("Error creating notification", e);
            throw new NotificationException("Failed to create notification: " + e.getMessage());
        }
    }

    private void sendNotification(Notification notification) {
        String channel = notification.getChannel();
        if ("EMAIL".equalsIgnoreCase(channel) && notification.getRecipientEmail() != null) {
            Map<String, Object> variables = new HashMap<>();
            variables.put("title", notification.getTitle());
            variables.put("message", notification.getMessage());
            emailService.sendHtmlEmail(notification.getRecipientEmail(), notification.getTitle(), "email-template", variables);
        } else if ("PUSH".equalsIgnoreCase(channel)) {
            List<UserPushToken> tokens = pushTokenRepository.findByUserId(notification.getUserId());
            for (UserPushToken token : tokens) {
                pushNotificationService.sendPushNotification(token.getToken(), notification.getTitle(), notification.getMessage());
            }
        }
    }

    @Override
    public void registerPushToken(String userId, String token, String deviceType) {
        pushTokenRepository.findByUserIdAndToken(userId, token)
            .ifPresentOrElse(
                t -> {
                    t.setDeviceType(deviceType);
                    pushTokenRepository.save(t);
                },
                () -> {
                    UserPushToken newToken = UserPushToken.builder()
                        .userId(userId)
                        .token(token)
                        .deviceType(deviceType)
                        .build();
                    pushTokenRepository.save(newToken);
                }
            );
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
    public Page<NotificationResponse> getNotificationsByUserId(String userId, Pageable pageable) {
        return notificationRepository.findByUserId(userId, pageable)
                .map(NotificationResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUnreadNotificationsByUserId(String userId, Pageable pageable) {
        return notificationRepository.findByUserIdAndIsRead(userId, false, pageable)
                .map(NotificationResponse::fromEntity);
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
    public Page<NotificationResponse> getRecentNotifications(String userId, int days, Pageable pageable) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return notificationRepository.findRecentNotifications(userId, since, pageable)
                .map(NotificationResponse::fromEntity);
    }
}
