package com.rudraksha.shopsphere.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitingService {

    private static final String LOGIN_PREFIX = "rate_limit:login:";
    private static final String REGISTER_PREFIX = "rate_limit:register:";
    private static final String FORGOT_PASSWORD_PREFIX = "rate_limit:forgot:";

    private final RedisTemplate<String, String> redisTemplate;

    public boolean isLoginAllowed(String email) {
        return isAllowed(LOGIN_PREFIX + email, 5, 60);
    }

    public boolean isRegisterAllowed(String email) {
        return isAllowed(REGISTER_PREFIX + email, 3, 3600);
    }

    public boolean isForgotPasswordAllowed(String email) {
        return isAllowed(FORGOT_PASSWORD_PREFIX + email, 3, 3600);
    }

    private boolean isAllowed(String key, int maxAttempts, int windowSeconds) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == 1) {
                redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
            }
            boolean allowed = count <= maxAttempts;
            if (!allowed) {
                log.warn("Rate limit exceeded for key: {}", key.replaceAll(":.*@", ":***@"));
            }
            return allowed;
        } catch (Exception e) {
            log.error("Rate limiting check failed, allowing request", e);
            return true;
        }
    }

    public void resetLoginAttempts(String email) {
        try {
            redisTemplate.delete(LOGIN_PREFIX + email);
        } catch (Exception e) {
            log.error("Failed to reset login attempts for email: {}", email, e);
        }
    }
}
