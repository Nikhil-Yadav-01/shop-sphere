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
            // Wait up to 3 seconds, watchdog auto-renews (no lease time specified)
            if (lock.tryLock(3, TimeUnit.SECONDS)) {
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

    /**
     * Execute operation with multiple distributed locks in a sorted order to prevent deadlocks.
     * @param skus List of Stock Keeping Units to lock
     * @param operation Callback to execute under locks
     * @return Result from operation
     * @throws RuntimeException if any lock cannot be acquired
     */
    public <T> T executeWithLocks(java.util.List<String> skus, LockCallback<T> operation) {
        if (skus == null || skus.isEmpty()) {
            return operation.execute();
        }
        
        // Sort to prevent distributed deadlocks
        java.util.List<String> sortedSkus = skus.stream()
            .distinct()
            .sorted()
            .collect(java.util.stream.Collectors.toList());
            
        java.util.List<RLock> acquiredLocks = new java.util.ArrayList<>();
        
        try {
            for (String sku : sortedSkus) {
                String lockKey = LOCK_PREFIX + sku;
                RLock lock = redissonClient.getLock(lockKey);
                // Wait up to 3 seconds, watchdog auto-renews
                if (lock.tryLock(3, TimeUnit.SECONDS)) {
                    acquiredLocks.add(lock);
                    log.debug("Lock acquired for SKU: {}", sku);
                } else {
                    log.warn("Failed to acquire lock for SKU: {} after 3 seconds", sku);
                    throw new RuntimeException("Could not acquire lock for SKU: " + sku);
                }
            }
            return operation.execute();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Multi-lock acquisition interrupted");
            throw new RuntimeException("Lock acquisition interrupted", e);
        } finally {
            // Release in reverse order of acquisition
            for (int i = acquiredLocks.size() - 1; i >= 0; i--) {
                RLock lock = acquiredLocks.get(i);
                try {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                        log.debug("Lock released for SKU: {}", lock.getName());
                    }
                } catch (Exception e) {
                    log.error("Failed to release lock cleanly for " + lock.getName(), e);
                }
            }
        }
    }

    @FunctionalInterface
    public interface LockCallback<T> {
        T execute();
    }
}
