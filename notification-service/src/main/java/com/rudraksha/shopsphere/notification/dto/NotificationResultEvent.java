package com.rudraksha.shopsphere.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResultEvent {
    private String correlationId;
    private String status; // SUCCESS or FAILED
    private String errorReason;
}
