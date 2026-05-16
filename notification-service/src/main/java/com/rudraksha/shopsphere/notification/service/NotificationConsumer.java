package com.rudraksha.shopsphere.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rudraksha.shopsphere.notification.dto.EmailNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final EmailService emailService;
    private final PushNotificationService pushNotificationService;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = ".dlt"
    )
    @KafkaListener(topics = "notification.email.send", groupId = "notification-group")
    public void consumeEmailNotificationEvent(String message) {
        try {
            EmailNotificationEvent event = objectMapper.readValue(message, EmailNotificationEvent.class);
            log.info("Processing email notification for: {}", event.getTo());
            
            java.util.Map<String, Object> variables = new java.util.HashMap<>();
            variables.put("title", event.getSubject());
            variables.put("message", event.getBody());
            
            emailService.sendHtmlEmail(event.getTo(), event.getSubject(), "email-template", variables);
        } catch (Exception e) {
            log.error("Failed to process email notification: {}. Error: {}", message, e.getMessage());
            throw new RuntimeException("Email processing failed, triggering retry", e);
        }
    }

    @DltHandler
    public void handleDlt(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("Event from topic {} moved to DLT: {}", topic, message);
        // Here you could save to a database for manual intervention
    }
}
