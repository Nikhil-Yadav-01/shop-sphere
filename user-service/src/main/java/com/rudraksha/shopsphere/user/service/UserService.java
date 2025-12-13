package com.rudraksha.shopsphere.user.service;

import com.rudraksha.shopsphere.user.dto.request.UpdateUserRequest;
import com.rudraksha.shopsphere.user.dto.response.UserResponse;

import java.util.UUID;

public interface UserService {

    UserResponse getUserProfile(UUID userId);

    UserResponse createUserProfile(UUID authUserId);

    UserResponse updateUserProfile(UUID userId, UpdateUserRequest request);

    void deleteUserProfile(UUID userId);

    boolean userExists(UUID authUserId);
}
