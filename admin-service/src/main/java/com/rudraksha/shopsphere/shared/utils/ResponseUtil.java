package com.rudraksha.shopsphere.shared.utils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class ResponseUtil {

    private ResponseUtil() {
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Success", data, null, Instant.now());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(String message, Map<String, String> errors) {
        return new ApiResponse<>(false, message, null, errors, Instant.now());
    }

    public static <T> ApiResponse<T> notFound(String resource) {
        return new ApiResponse<>(false, resource + " not found", null, null, Instant.now());
    }

    public static <T> ApiResponse<T> notFound(String resource, Object id) {
        return new ApiResponse<>(false, resource + " not found with id: " + id, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> validationError(String field, String message) {
        Map<String, String> errors = new HashMap<>();
        errors.put(field, message);
        return new ApiResponse<>(false, "Validation failed", null, errors, Instant.now());
    }

    public record ApiResponse<T>(
            boolean success,
            String message,
            T data,
            Map<String, String> errors,
            Instant timestamp
    ) {
    }
}
