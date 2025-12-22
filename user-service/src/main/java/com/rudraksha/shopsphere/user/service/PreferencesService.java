package com.rudraksha.shopsphere.user.service;

import com.rudraksha.shopsphere.user.dto.request.UpdatePreferencesRequest;
import com.rudraksha.shopsphere.user.dto.response.PreferencesResponse;

import java.util.UUID;

public interface PreferencesService {

    PreferencesResponse getPreferences(UUID userId);

    PreferencesResponse updatePreferences(UUID userId, UpdatePreferencesRequest request);
}
