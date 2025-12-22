package com.rudraksha.shopsphere.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePreferencesRequest {

    private Boolean newsletterSubscription;

    private Boolean marketingEmails;

    private Boolean orderNotifications;

    private String notificationLanguage;
}
