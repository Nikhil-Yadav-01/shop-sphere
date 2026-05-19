package com.rudraksha.shopsphere.auth.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationEvent {
    private String to;
    private String subject;
    private String body;
    private String templateName;
    private Map<String, Object> templateVariables;
    private String replyToTopic;
    private String correlationId;
}
