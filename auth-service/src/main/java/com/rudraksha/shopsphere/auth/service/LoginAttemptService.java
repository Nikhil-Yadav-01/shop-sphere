package com.rudraksha.shopsphere.auth.service;

import com.rudraksha.shopsphere.auth.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_TIME_MINUTES = 15;

    public void loginSucceeded(String email) {
        String key = getCacheKey(email);
        redisTemplate.delete(key);
    }

    public void loginFailed(String email) {
        String key = getCacheKey(email);
        Long attempts = redisTemplate.opsForValue().increment(key);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(key, LOCK_TIME_MINUTES, TimeUnit.MINUTES);
        }
    }

    public void checkLockout(String email) {
        String key = getCacheKey(email);
        String attempts = redisTemplate.opsForValue().get(key);
        if (attempts != null && Integer.parseInt(attempts) >= MAX_ATTEMPTS) {
            log.warn("Account is locked out for email: {}", email);
            throw new AuthException("Account is locked due to too many failed login attempts. Please try again in 15 minutes.");
        }
    }

    private String getCacheKey(String email) {
        return "login_attempts:" + email;
    }
}
