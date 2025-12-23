package com.rudraksha.shopsphere.inventory.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Utility for distributed locking using Redis.
 * Prevents race conditions in concurrent inventory operations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockUtil {
    private final StringRedisTemplate redisTemplate;
    private static final String LOCK_PREFIX = "inventory:lock:";
    private static final long LOCK_TIMEOUT_MS = 5000; // 5 seconds
    private static final long LOCK_RETRY_INTERVAL_MS = 100;

    /**
     * Acquire a distributed lock for inventory operation
     * @param sku Stock Keeping Unit
     * @return Lock token if acquired, null if failed
     */
    public String acquireLock(String sku) {
        String lockKey = LOCK_PREFIX + sku;
        String lockToken = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < LOCK_TIMEOUT_MS) {
            Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockToken, 5, TimeUnit.SECONDS);
            
            if (success != null && success) {
                log.debug("Lock acquired for SKU: {} with token: {}", sku, lockToken);
                return lockToken;
            }
            
            try {
                Thread.sleep(LOCK_RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Lock acquisition interrupted for SKU: {}", sku);
                return null;
            }
        }
        
        log.warn("Failed to acquire lock for SKU: {} after {} ms", sku, LOCK_TIMEOUT_MS);
        return null;
    }

    /**
     * Release a distributed lock
     * @param sku Stock Keeping Unit
     * @param lockToken Token returned from acquireLock
     */
    public void releaseLock(String sku, String lockToken) {
        if (lockToken == null) {
            return;
        }
        
        String lockKey = LOCK_PREFIX + sku;
        
        // Verify token before releasing (prevents releasing someone else's lock)
        String currentToken = redisTemplate.opsForValue().get(lockKey);
        
        if (lockToken.equals(currentToken)) {
            redisTemplate.delete(lockKey);
            log.debug("Lock released for SKU: {}", sku);
        } else {
            log.warn("Lock token mismatch for SKU: {}. Not releasing.", sku);
        }
    }

    /**
     * Execute operation with distributed lock
     * @param sku Stock Keeping Unit
     * @param operation Callback to execute under lock
     * @return Result from operation
     * @throws RuntimeException if lock cannot be acquired
     */
    public <T> T executeWithLock(String sku, LockCallback<T> operation) {
        String lockToken = acquireLock(sku);
        
        if (lockToken == null) {
            throw new RuntimeException("Could not acquire lock for SKU: " + sku);
        }
        
        try {
            return operation.execute();
        } finally {
            releaseLock(sku, lockToken);
        }
    }

    @FunctionalInterface
    public interface LockCallback<T> {
        T execute();
    }
}
