package com.rudraksha.shopsphere.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferencesResponse {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("newsletter_subscription")
    private Boolean newsletterSubscription;

    @JsonProperty("marketing_emails")
    private Boolean marketingEmails;

    @JsonProperty("order_notifications")
    private Boolean orderNotifications;

    @JsonProperty("notification_language")
    private String notificationLanguage;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
