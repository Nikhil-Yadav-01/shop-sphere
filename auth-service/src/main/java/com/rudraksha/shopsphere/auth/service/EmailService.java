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

    public void sendVerificationEmail(String toEmail, String name, String token) {
        try {
            String verificationUrl = baseUrl + "/auth/verify-email?token=" + token;
            
            java.util.Map<String, Object> variables = new java.util.HashMap<>();
            variables.put("name", name);
            variables.put("verificationUrl", verificationUrl);

            EmailNotificationEvent event = EmailNotificationEvent.builder()
                    .to(toEmail)
                    .subject("Verify your email - ShopSphere")
                    .templateName("verification-email")
                    .templateVariables(variables)
                    .build();

            kafkaTemplate.send("notification.email.send", toEmail, event);
            log.info("Published verification email event to Kafka for: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to publish verification email event for: {}", toEmail, e);
        }
    }

    public void sendPasswordResetEmail(String toEmail, String name, String token) {
        try {
            String resetUrl = baseUrl + "/auth/reset-password?token=" + token;

            java.util.Map<String, Object> variables = new java.util.HashMap<>();
            variables.put("name", name);
            variables.put("resetUrl", resetUrl);

            EmailNotificationEvent event = EmailNotificationEvent.builder()
                    .to(toEmail)
                    .subject("Password Reset - ShopSphere")
                    .templateName("password-reset")
                    .templateVariables(variables)
                    .build();

            kafkaTemplate.send("notification.email.send", toEmail, event);
            log.info("Published password reset email event to Kafka for: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to publish password reset email event for: {}", toEmail, e);
        }
    }

    public void sendWelcomeEmail(String toEmail, String name) {
        try {
            java.util.Map<String, Object> variables = new java.util.HashMap<>();
            variables.put("name", name);

            EmailNotificationEvent event = EmailNotificationEvent.builder()
                    .to(toEmail)
                    .subject("Welcome to ShopSphere!")
                    .templateName("welcome-email")
                    .templateVariables(variables)
                    .build();

            kafkaTemplate.send("notification.email.send", toEmail, event);
            log.info("Published welcome email event to Kafka for: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to publish welcome email event for: {}", toEmail, e);
        }
    }
}