package com.rudraksha.shopsphere.auth.service;

import com.rudraksha.shopsphere.auth.exception.AuthException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private LoginAttemptService loginAttemptService;

    @Test
    void loginSucceeded_DeletesKey() {
        String email = "test@example.com";
        
        loginAttemptService.loginSucceeded(email);
        
        verify(redisTemplate).delete("login_attempts:" + email);
    }

    @Test
    void loginFailed_FirstTimeSetsExpiry() {
        String email = "test@example.com";
        String key = "login_attempts:" + email;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(key)).thenReturn(1L);

        loginAttemptService.loginFailed(email);

        verify(valueOperations).increment(key);
        verify(redisTemplate).expire(key, 15, TimeUnit.MINUTES);
    }

    @Test
    void loginFailed_SubsequentTimesDoesNotSetExpiry() {
        String email = "test@example.com";
        String key = "login_attempts:" + email;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(key)).thenReturn(2L);

        loginAttemptService.loginFailed(email);

        verify(valueOperations).increment(key);
        verify(redisTemplate, never()).expire(eq(key), anyLong(), any(TimeUnit.class));
    }

    @Test
    void checkLockout_NotLocked() {
        String email = "test@example.com";
        String key = "login_attempts:" + email;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(null);

        assertDoesNotThrow(() -> loginAttemptService.checkLockout(email));
    }

    @Test
    void checkLockout_BelowMaxAttempts() {
        String email = "test@example.com";
        String key = "login_attempts:" + email;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn("4");

        assertDoesNotThrow(() -> loginAttemptService.checkLockout(email));
    }

    @Test
    void checkLockout_Locked() {
        String email = "test@example.com";
        String key = "login_attempts:" + email;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn("5");

        AuthException exception = assertThrows(AuthException.class, () -> loginAttemptService.checkLockout(email));
        assertTrue(exception.getMessage().contains("locked"));
    }
}
