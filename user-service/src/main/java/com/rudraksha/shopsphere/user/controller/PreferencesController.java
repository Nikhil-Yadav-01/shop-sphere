package com.rudraksha.shopsphere.user.controller;

import com.rudraksha.shopsphere.user.dto.request.UpdatePreferencesRequest;
import com.rudraksha.shopsphere.user.dto.response.PreferencesResponse;
import com.rudraksha.shopsphere.user.service.PreferencesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/users/{userId}/preferences")
@RequiredArgsConstructor
public class PreferencesController {

    private final PreferencesService preferencesService;

    @GetMapping
    public ResponseEntity<PreferencesResponse> getPreferences(@PathVariable UUID userId) {
        log.info("GET /api/v1/users/{}/preferences - Fetching user preferences", userId);
        PreferencesResponse response = preferencesService.getPreferences(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<PreferencesResponse> updatePreferences(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdatePreferencesRequest request) {
        log.info("PUT /api/v1/users/{}/preferences - Updating user preferences", userId);
        PreferencesResponse response = preferencesService.updatePreferences(userId, request);
        return ResponseEntity.ok(response);
    }
}
