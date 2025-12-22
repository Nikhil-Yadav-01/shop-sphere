package com.rudraksha.shopsphere.user.service.impl;

import com.rudraksha.shopsphere.user.dto.request.UpdatePreferencesRequest;
import com.rudraksha.shopsphere.user.dto.response.PreferencesResponse;
import com.rudraksha.shopsphere.user.entity.Preferences;
import com.rudraksha.shopsphere.user.entity.UserProfile;
import com.rudraksha.shopsphere.user.mapper.PreferencesMapper;
import com.rudraksha.shopsphere.user.repository.PreferencesRepository;
import com.rudraksha.shopsphere.user.repository.UserProfileRepository;
import com.rudraksha.shopsphere.user.service.PreferencesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PreferencesServiceImpl implements PreferencesService {

    private final PreferencesRepository preferencesRepository;
    private final UserProfileRepository userProfileRepository;
    private final PreferencesMapper preferencesMapper;

    @Override
    @Transactional(readOnly = true)
    public PreferencesResponse getPreferences(UUID userId) {
        log.debug("Fetching preferences for userId: {}", userId);

        UserProfile userProfile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"));

        Preferences preferences = userProfile.getPreferences();
        if (preferences == null) {
            throw new RuntimeException("Preferences not found for user");
        }

        return preferencesMapper.toPreferencesResponse(preferences);
    }

    @Override
    public PreferencesResponse updatePreferences(UUID userId, UpdatePreferencesRequest request) {
        log.debug("Updating preferences for userId: {}", userId);

        UserProfile userProfile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"));

        Preferences preferences = userProfile.getPreferences();
        if (preferences == null) {
            throw new RuntimeException("Preferences not found for user");
        }

        if (request.getNewsletterSubscription() != null) {
            preferences.setNewsletterSubscription(request.getNewsletterSubscription());
        }
        if (request.getMarketingEmails() != null) {
            preferences.setMarketingEmails(request.getMarketingEmails());
        }
        if (request.getOrderNotifications() != null) {
            preferences.setOrderNotifications(request.getOrderNotifications());
        }
        if (request.getNotificationLanguage() != null) {
            preferences.setNotificationLanguage(request.getNotificationLanguage());
        }

        Preferences updatedPreferences = preferencesRepository.save(preferences);
        log.info("Preferences updated for userId: {}", userId);

        return preferencesMapper.toPreferencesResponse(updatedPreferences);
    }
}
