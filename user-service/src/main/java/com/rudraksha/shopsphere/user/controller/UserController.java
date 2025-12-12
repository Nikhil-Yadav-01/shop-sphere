package com.rudraksha.shopsphere.user.controller;

import com.rudraksha.shopsphere.user.dto.request.UpdateUserRequest;
import com.rudraksha.shopsphere.user.dto.response.UserResponse;
import com.rudraksha.shopsphere.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserProfile(@PathVariable UUID userId) {
        log.info("GET /api/v1/users/{} - Fetching user profile", userId);
        UserResponse response = userService.getUserProfile(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUserProfile(@RequestParam UUID authUserId) {
        log.info("POST /api/v1/users - Creating user profile for authUserId: {}", authUserId);
        UserResponse response = userService.createUserProfile(authUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUserProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request) {
        log.info("PUT /api/v1/users/{} - Updating user profile", userId);
        UserResponse response = userService.updateUserProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUserProfile(@PathVariable UUID userId) {
        log.info("DELETE /api/v1/users/{} - Deleting user profile", userId);
        userService.deleteUserProfile(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/exists/{authUserId}")
    public ResponseEntity<Boolean> userExists(@PathVariable UUID authUserId) {
        log.info("GET /api/v1/users/exists/{} - Checking if user exists", authUserId);
        boolean exists = userService.userExists(authUserId);
        return ResponseEntity.ok(exists);
    }
}
