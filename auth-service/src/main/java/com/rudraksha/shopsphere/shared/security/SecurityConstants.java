package com.rudraksha.shopsphere.shared.security;

public final class SecurityConstants {

    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
    public static final long TOKEN_EXPIRATION_MS = 86400000L; // 24 hours

    private SecurityConstants() {
    }
}
