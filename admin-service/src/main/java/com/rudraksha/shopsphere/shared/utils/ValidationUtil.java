package com.rudraksha.shopsphere.shared.utils;

import java.util.regex.Pattern;

public final class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[1-9]\\d{1,14}$"
    );

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    private ValidationUtil() {
    }

    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        String cleaned = phone.replaceAll("[\\s()-]", "");
        return PHONE_PATTERN.matcher(cleaned).matches();
    }

    public static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static boolean isValidUuid(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return false;
        }
        return UUID_PATTERN.matcher(uuid.trim()).matches();
    }

    public static boolean isPositive(Number number) {
        if (number == null) {
            return false;
        }
        return number.doubleValue() > 0;
    }

    public static boolean isNonNegative(Number number) {
        if (number == null) {
            return false;
        }
        return number.doubleValue() >= 0;
    }

    public static boolean hasMinLength(String value, int minLength) {
        if (value == null) {
            return false;
        }
        return value.length() >= minLength;
    }

    public static boolean hasMaxLength(String value, int maxLength) {
        if (value == null) {
            return true;
        }
        return value.length() <= maxLength;
    }

    public static boolean isInRange(Number value, double min, double max) {
        if (value == null) {
            return false;
        }
        double v = value.doubleValue();
        return v >= min && v <= max;
    }
}
