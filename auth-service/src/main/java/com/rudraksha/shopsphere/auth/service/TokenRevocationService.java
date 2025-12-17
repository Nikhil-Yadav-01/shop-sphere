package com.rudraksha.shopsphere.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRevocationService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String REVOKED_TOKEN_PREFIX = "revoked_token:";

    public void revokeToken(String token, long expirationTimeInSeconds) {
        try {
            String key = REVOKED_TOKEN_PREFIX + token;
            redisTemplate.opsForValue().set(key, "revoked", Duration.ofSeconds(expirationTimeInSeconds));
            log.info("Token revoked successfully");
        } catch (Exception e) {
            log.error("Failed to revoke token", e);
        }
    }

    public boolean isTokenRevoked(String token) {
        try {
            String key = REVOKED_TOKEN_PREFIX + token;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Failed to check token revocation status", e);
            return false;
        }
    }
}