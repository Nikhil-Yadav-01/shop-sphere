package com.rudraksha.shopsphere.user.kafka;

import com.rudraksha.shopsphere.user.event.UserProfileUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventPublisher {

    private final KafkaTemplate<String, UserProfileUpdatedEvent> kafkaTemplate;

    public void publishUserProfileUpdated(UserProfileUpdatedEvent event) {
        log.info("Publishing user.profile.updated event for userId: {}", event.getUserId());
        kafkaTemplate.send("user.profile.updated", event.getUserId().toString(), event);
    }
}
