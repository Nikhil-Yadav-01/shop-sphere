package com.rudraksha.shopsphere.inventory.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Utility for distributed locking using Redisson.
 * Provides safe locking with watchdog mechanism.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockUtil {
    private final RedissonClient redissonClient;
    private static final String LOCK_PREFIX = "inventory:lock:";

    /**
     * Execute operation with distributed lock
     * @param sku Stock Keeping Unit
     * @param operation Callback to execute under lock
     * @return Result from operation
     * @throws RuntimeException if lock cannot be acquired
     */
    public <T> T executeWithLock(String sku, LockCallback<T> operation) {
        String lockKey = LOCK_PREFIX + sku;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // Wait up to 3 seconds, auto-release after 30 seconds (watchdog auto-renews)
            if (lock.tryLock(3, 30, TimeUnit.SECONDS)) {
                try {
                    log.debug("Lock acquired for SKU: {}", sku);
                    return operation.execute();
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                        log.debug("Lock released for SKU: {}", sku);
                    }
                }
            } else {
                log.warn("Failed to acquire lock for SKU: {} after 3 seconds", sku);
                throw new RuntimeException("Could not acquire lock for SKU: " + sku);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Lock acquisition interrupted for SKU: {}", sku);
            throw new RuntimeException("Lock acquisition interrupted for SKU: " + sku, e);
        }
    }

    @FunctionalInterface
    public interface LockCallback<T> {
        T execute();
    }
}
