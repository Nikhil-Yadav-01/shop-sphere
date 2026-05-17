package com.rudraksha.shopsphere.auth.service;

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
class RateLimitingServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RateLimitingService rateLimitingService;

    @Test
    void isLoginAllowed_Allowed() {
        String email = "test@example.com";
        String key = "rate_limit:login:" + email;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(key)).thenReturn(1L);

        assertTrue(rateLimitingService.isLoginAllowed(email));
        verify(redisTemplate).expire(key, 60, TimeUnit.SECONDS);
    }

    @Test
    void isLoginAllowed_Exceeded() {
        String email = "test@example.com";
        String key = "rate_limit:login:" + email;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(key)).thenReturn(6L);

        assertFalse(rateLimitingService.isLoginAllowed(email));
        verify(redisTemplate, never()).expire(eq(key), anyLong(), any(TimeUnit.class));
    }

    @Test
    void isRegisterAllowed_Allowed() {
        String email = "test@example.com";
        String key = "rate_limit:register:" + email;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(key)).thenReturn(1L);

        assertTrue(rateLimitingService.isRegisterAllowed(email));
        verify(redisTemplate).expire(key, 3600, TimeUnit.SECONDS);
    }

    @Test
    void isForgotPasswordAllowed_Allowed() {
        String email = "test@example.com";
        String key = "rate_limit:forgot:" + email;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(key)).thenReturn(1L);

        assertTrue(rateLimitingService.isForgotPasswordAllowed(email));
        verify(redisTemplate).expire(key, 3600, TimeUnit.SECONDS);
    }

    @Test
    void isAllowed_FallbackOnException() {
        String email = "test@example.com";
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis down"));

        // Rate limiter should fail-open (allow requests if Redis is down)
        assertTrue(rateLimitingService.isLoginAllowed(email));
    }

    @Test
    void resetLoginAttempts_Success() {
        String email = "test@example.com";
        
        rateLimitingService.resetLoginAttempts(email);

        verify(redisTemplate).delete("rate_limit:login:" + email);
    }
}
