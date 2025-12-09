package com.rudraksha.shopsphere.shared.utils;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

public final class RetryUtil {

    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final Duration DEFAULT_DELAY = Duration.ofMillis(500);
    private static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;

    private RetryUtil() {
    }

    public static <T> T retry(Callable<T> operation) throws Exception {
        return retry(operation, DEFAULT_MAX_ATTEMPTS, DEFAULT_DELAY);
    }

    public static <T> T retry(Callable<T> operation, int maxAttempts) throws Exception {
        return retry(operation, maxAttempts, DEFAULT_DELAY);
    }

    public static <T> T retry(Callable<T> operation, int maxAttempts, Duration delay) throws Exception {
        return retry(operation, maxAttempts, delay, e -> true);
    }

    public static <T> T retry(Callable<T> operation, int maxAttempts, Duration delay,
                              Predicate<Exception> retryOn) throws Exception {
        if (operation == null) {
            throw new IllegalArgumentException("Operation cannot be null");
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("Max attempts must be at least 1");
        }

        Exception lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.call();
            } catch (Exception e) {
                lastException = e;
                if (attempt >= maxAttempts || !retryOn.test(e)) {
                    throw e;
                }
                sleep(delay);
            }
        }
        throw lastException;
    }

    public static <T> T retryWithBackoff(Callable<T> operation, int maxAttempts, Duration initialDelay) throws Exception {
        return retryWithBackoff(operation, maxAttempts, initialDelay, DEFAULT_BACKOFF_MULTIPLIER);
    }

    public static <T> T retryWithBackoff(Callable<T> operation, int maxAttempts,
                                         Duration initialDelay, double backoffMultiplier) throws Exception {
        if (operation == null) {
            throw new IllegalArgumentException("Operation cannot be null");
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("Max attempts must be at least 1");
        }
        if (backoffMultiplier < 1.0) {
            throw new IllegalArgumentException("Backoff multiplier must be at least 1.0");
        }

        Exception lastException = null;
        Duration currentDelay = initialDelay;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.call();
            } catch (Exception e) {
                lastException = e;
                if (attempt >= maxAttempts) {
                    throw e;
                }
                sleep(currentDelay);
                currentDelay = Duration.ofMillis((long) (currentDelay.toMillis() * backoffMultiplier));
            }
        }
        throw lastException;
    }

    @SafeVarargs
    public static <T> T retryOnExceptions(Callable<T> operation, int maxAttempts, Duration delay,
                                          Class<? extends Exception>... retryableExceptions) throws Exception {
        Set<Class<? extends Exception>> retryable = Set.of(retryableExceptions);
        return retry(operation, maxAttempts, delay, e ->
                retryable.stream().anyMatch(clazz -> clazz.isInstance(e))
        );
    }

    public static void retryRunnable(Runnable operation, int maxAttempts, Duration delay) throws Exception {
        retry(() -> {
            operation.run();
            return null;
        }, maxAttempts, delay);
    }

    private static void sleep(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return;
        }
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RetryInterruptedException("Retry interrupted", e);
        }
    }

    public static class RetryInterruptedException extends RuntimeException {
        public RetryInterruptedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
