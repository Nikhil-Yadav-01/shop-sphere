package com.rudraksha.shopsphere.auth.service;

import com.rudraksha.shopsphere.auth.dto.kafka.EmailNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.verification.base-url}")
    private String baseUrl;

    public void sendVerificationEmail(String toEmail, String token) {
        try {
            String verificationUrl = baseUrl + "/auth/verify-email?token=" + token;
            String text = "Please click the following link to verify your email: " + verificationUrl;

            EmailNotificationEvent event = EmailNotificationEvent.builder()
                    .to(toEmail)
                    .subject("Email Verification - ShopSphere")
                    .body(text)
                    .build();

            kafkaTemplate.send("notification.email.send", toEmail, event);
            log.info("Published verification email event to Kafka for: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to publish verification email event for: {}", toEmail, e);
        }
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            String resetUrl = baseUrl + "/auth/reset-password?token=" + token;
            String text = "Please click the following link to reset your password: " + resetUrl;

            EmailNotificationEvent event = EmailNotificationEvent.builder()
                    .to(toEmail)
                    .subject("Password Reset - ShopSphere")
                    .body(text)
                    .build();

            kafkaTemplate.send("notification.email.send", toEmail, event);
            log.info("Published password reset email event to Kafka for: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to publish password reset email event for: {}", toEmail, e);
        }
    }
}