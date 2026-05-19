package com.rudraksha.shopsphere.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenRevocationServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenRevocationService tokenRevocationService;

    @Test
    void revokeToken_Success() {
        String token = "access_token";
        String key = "revoked_token:" + token;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        tokenRevocationService.revokeToken(token, 3600L);

        verify(valueOperations).set(eq(key), eq("revoked"), eq(Duration.ofSeconds(3600L)));
    }

    @Test
    void isTokenRevoked_True() {
        String token = "revoked_token";
        String key = "revoked_token:" + token;
        when(redisTemplate.hasKey(key)).thenReturn(true);

        assertTrue(tokenRevocationService.isTokenRevoked(token));
    }

    @Test
    void isTokenRevoked_False() {
        String token = "valid_token";
        String key = "revoked_token:" + token;
        when(redisTemplate.hasKey(key)).thenReturn(false);

        assertFalse(tokenRevocationService.isTokenRevoked(token));
    }

    @Test
    void isTokenRevoked_FallbackOnException() {
        String token = "token";
        when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Redis down"));

        assertFalse(tokenRevocationService.isTokenRevoked(token));
    }
}
