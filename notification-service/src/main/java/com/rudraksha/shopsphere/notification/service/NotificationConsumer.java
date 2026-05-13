package com.rudraksha.shopsphere.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rudraksha.shopsphere.notification.dto.EmailNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "notification.email.send", groupId = "notification-group")
    public void consumeEmailNotificationEvent(String message) {
        try {
            EmailNotificationEvent event = objectMapper.readValue(message, EmailNotificationEvent.class);
            log.info("Received email notification event for: {}", event.getTo());
            emailService.sendEmail(event.getTo(), event.getSubject(), event.getBody());
        } catch (Exception e) {
            log.error("Failed to process email notification event: {}", message, e);
        }
    }
}
