package com.rudraksha.shopsphere.user.service.impl;

import com.rudraksha.shopsphere.user.dto.request.UpdateUserRequest;
import com.rudraksha.shopsphere.user.dto.response.UserResponse;
import com.rudraksha.shopsphere.user.entity.UserProfile;
import com.rudraksha.shopsphere.user.event.UserProfileUpdatedEvent;
import com.rudraksha.shopsphere.user.kafka.UserEventPublisher;
import com.rudraksha.shopsphere.user.mapper.UserMapper;
import com.rudraksha.shopsphere.user.repository.UserProfileRepository;
import com.rudraksha.shopsphere.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserProfileRepository userProfileRepository;
    private final UserMapper userMapper;
    private final UserEventPublisher userEventPublisher;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserProfile(UUID userId) {
        log.debug("Fetching user profile for userId: {}", userId);
        UserProfile userProfile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"));
        return userMapper.toUserResponse(userProfile);
    }

    @Override
    public UserResponse createUserProfile(UUID authUserId) {
        log.debug("Creating user profile for authUserId: {}", authUserId);
        
        if (userProfileRepository.existsByAuthUserId(authUserId)) {
            throw new RuntimeException("User profile already exists for this auth user");
        }

        UserProfile userProfile = UserProfile.builder()
                .authUserId(authUserId)
                .build();

        UserProfile savedProfile = userProfileRepository.save(userProfile);
        log.info("User profile created with id: {}", savedProfile.getId());
        return userMapper.toUserResponse(savedProfile);
    }

    @Override
    public UserResponse updateUserProfile(UUID userId, UpdateUserRequest request) {
        log.debug("Updating user profile for userId: {}", userId);
        UserProfile userProfile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"));

        if (request.getPhone() != null) {
            userProfile.setPhone(request.getPhone());
        }
        if (request.getDateOfBirth() != null) {
            userProfile.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getAvatarUrl() != null) {
            userProfile.setAvatarUrl(request.getAvatarUrl());
        }

        UserProfile updatedProfile = userProfileRepository.save(userProfile);
        log.info("User profile updated with id: {}", updatedProfile.getId());

        // Publish user profile updated event
        UserProfileUpdatedEvent event = UserProfileUpdatedEvent.builder()
                .userId(updatedProfile.getAuthUserId())
                .profileId(updatedProfile.getId())
                .phone(updatedProfile.getPhone())
                .avatarUrl(updatedProfile.getAvatarUrl())
                .updatedAt(updatedProfile.getUpdatedAt())
                .build();
        userEventPublisher.publishUserProfileUpdated(event);

        return userMapper.toUserResponse(updatedProfile);
    }

    @Override
    public void deleteUserProfile(UUID userId) {
        log.debug("Deleting user profile for userId: {}", userId);
        if (!userProfileRepository.existsById(userId)) {
            throw new RuntimeException("User profile not found");
        }
        userProfileRepository.deleteById(userId);
        log.info("User profile deleted with id: {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean userExists(UUID authUserId) {
        return userProfileRepository.existsByAuthUserId(authUserId);
    }
}
