package com.rudraksha.shopsphere.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStateNotificationEvent {
    private String orderId;
    private String userId;
    private String newStatus;
    private String previousStatus;
    private String timestamp;
}
