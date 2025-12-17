package com.rudraksha.shopsphere.auth.kafka;

import com.rudraksha.shopsphere.auth.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishUserCreatedEvent(User user) {
        try {
            Map<String, Object> event = Map.of(
                "eventType", "USER_CREATED",
                "userId", user.getId().toString(),
                "email", user.getEmail(),
                "firstName", user.getFirstName(),
                "lastName", user.getLastName(),
                "role", user.getRole().name(),
                "timestamp", LocalDateTime.now().toString()
            );

            kafkaTemplate.send("user.created", user.getId().toString(), event);
            log.info("Published user.created event for user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to publish user.created event for user: {}", user.getEmail(), e);
        }
    }

    public void publishUserUpdatedEvent(User user) {
        try {
            Map<String, Object> event = Map.of(
                "eventType", "USER_UPDATED",
                "userId", user.getId().toString(),
                "email", user.getEmail(),
                "firstName", user.getFirstName(),
                "lastName", user.getLastName(),
                "role", user.getRole().name(),
                "timestamp", LocalDateTime.now().toString()
            );

            kafkaTemplate.send("user.updated", user.getId().toString(), event);
            log.info("Published user.updated event for user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to publish user.updated event for user: {}", user.getEmail(), e);
        }
    }
}