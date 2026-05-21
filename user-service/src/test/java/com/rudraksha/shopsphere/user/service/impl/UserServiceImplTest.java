package com.rudraksha.shopsphere.user.service.impl;

import com.rudraksha.shopsphere.user.dto.request.UpdateUserRequest;
import com.rudraksha.shopsphere.user.dto.response.UserResponse;
import com.rudraksha.shopsphere.user.entity.UserProfile;
import com.rudraksha.shopsphere.user.event.UserProfileUpdatedEvent;
import com.rudraksha.shopsphere.user.kafka.UserEventPublisher;
import com.rudraksha.shopsphere.user.mapper.UserMapper;
import com.rudraksha.shopsphere.user.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserEventPublisher userEventPublisher;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID userId;
    private UserProfile userProfile;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userProfile = UserProfile.builder()
                .id(userId)
                .authUserId(UUID.randomUUID())
                .phone("1234567890")
                .build();
        
        userResponse = new UserResponse();
        userResponse.setId(userId);
        userResponse.setPhone("1234567890");
    }

    @Test
    void getUserProfile_Success() {
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(userProfile));
        when(userMapper.toUserResponse(userProfile)).thenReturn(userResponse);

        UserResponse result = userService.getUserProfile(userId);

        assertNotNull(result);
        assertEquals(userId, result.getId());
        verify(userProfileRepository).findById(userId);
    }

    @Test
    void getUserProfile_NotFound() {
        when(userProfileRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.getUserProfile(userId));
    }

    @Test
    void createUserProfile_Success() {
        UUID authUserId = UUID.randomUUID();
        when(userProfileRepository.existsByAuthUserId(authUserId)).thenReturn(false);
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(userProfile);
        when(userMapper.toUserResponse(any(UserProfile.class))).thenReturn(userResponse);

        UserResponse result = userService.createUserProfile(authUserId);

        assertNotNull(result);
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    void createUserProfile_AlreadyExists() {
        UUID authUserId = UUID.randomUUID();
        when(userProfileRepository.existsByAuthUserId(authUserId)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> userService.createUserProfile(authUserId));
    }

    @Test
    void updateUserProfile_Success() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setPhone("0987654321");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));

        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(userProfile));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(userProfile);
        when(userMapper.toUserResponse(any(UserProfile.class))).thenReturn(userResponse);

        UserResponse result = userService.updateUserProfile(userId, request);

        assertNotNull(result);
        verify(userProfileRepository).save(userProfile);
        verify(userEventPublisher).publishUserProfileUpdated(any(UserProfileUpdatedEvent.class));
    }

    @Test
    void deleteUserProfile_Success() {
        when(userProfileRepository.existsById(userId)).thenReturn(true);
        doNothing().when(userProfileRepository).deleteById(userId);

        assertDoesNotThrow(() -> userService.deleteUserProfile(userId));
        verify(userProfileRepository).deleteById(userId);
    }

    @Test
    void userExists_True() {
        UUID authUserId = UUID.randomUUID();
        when(userProfileRepository.existsByAuthUserId(authUserId)).thenReturn(true);

        assertTrue(userService.userExists(authUserId));
    }
}
