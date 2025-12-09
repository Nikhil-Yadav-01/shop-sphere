package com.rudraksha.shopsphere.shared.utils;

import java.security.SecureRandom;
import java.util.UUID;

public final class IdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int SHORT_ID_LENGTH = 8;
    private static final int DEFAULT_RANDOM_LENGTH = 12;

    private IdGenerator() {
    }

    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    public static String generateUUIDWithoutDashes() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String generateShortId() {
        return generateShortId(SHORT_ID_LENGTH);
    }

    public static String generateShortId(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be positive");
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    public static String generateNumericId(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be positive");
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    public static String generatePrefixedId(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return generateShortId(DEFAULT_RANDOM_LENGTH);
        }
        return prefix + "_" + generateShortId(DEFAULT_RANDOM_LENGTH);
    }

    public static String generateOrderId() {
        return "ORD-" + System.currentTimeMillis() + "-" + generateShortId(4);
    }

    public static String generateTransactionId() {
        return "TXN-" + generateUUIDWithoutDashes().substring(0, 16).toUpperCase();
    }

    public static String generateReferenceCode() {
        return generateShortId(6).toUpperCase();
    }

    public static UUID parseUUID(String uuidStr) {
        if (uuidStr == null || uuidStr.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(uuidStr.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
