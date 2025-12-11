package com.rudraksha.shopsphere.notification.dto;

import com.rudraksha.shopsphere.notification.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;
    private String userId;
    private String type;
    private String title;
    private String message;
    private Boolean isRead;
    private String channel;
    private String recipientEmail;
    private String recipientPhone;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;

    public static NotificationResponse fromEntity(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .channel(notification.getChannel())
                .recipientEmail(notification.getRecipientEmail())
                .recipientPhone(notification.getRecipientPhone())
                .status(notification.getStatus().toString())
                .createdAt(notification.getCreatedAt())
                .sentAt(notification.getSentAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
