package com.rudraksha.shopsphere.user.kafka;

import com.rudraksha.shopsphere.user.entity.Preferences;
import com.rudraksha.shopsphere.user.entity.UserProfile;
import com.rudraksha.shopsphere.user.event.UserCreatedEvent;
import com.rudraksha.shopsphere.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventListener {

    private final UserProfileRepository userProfileRepository;

    @KafkaListener(
            topics = "user.created",
            groupId = "user-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleUserCreatedEvent(UserCreatedEvent event) {
        log.info("Received user.created event for userId: {}", event.getUserId());

        // Check if profile already exists
        if (userProfileRepository.existsByAuthUserId(event.getUserId())) {
            log.warn("User profile already exists for authUserId: {}", event.getUserId());
            return;
        }

        // Create user profile
        UserProfile userProfile = UserProfile.builder()
                .authUserId(event.getUserId())
                .build();

        // Create default preferences
        Preferences preferences = Preferences.builder()
                .userProfile(userProfile)
                .newsletterSubscription(false)
                .marketingEmails(false)
                .orderNotifications(true)
                .notificationLanguage("en")
                .build();

        userProfile.setPreferences(preferences);

        UserProfile savedProfile = userProfileRepository.save(userProfile);
        log.info("User profile created from Kafka event for authUserId: {} with profileId: {}", 
                event.getUserId(), savedProfile.getId());
    }
}
