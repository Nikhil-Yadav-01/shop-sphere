package com.rudraksha.shopsphere.shared.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private static final String SECRET = "9a4f2c8d3b7a1e5f8g0h2i4k6m8n0p2q4r6s8t0u2v4w6x8y0z2a4b6c8d0e2f4g";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, 3600000L);
    }

    @Test
    void generateAndValidateToken_Success() {
        String userId = "user-123";
        String email = "test@example.com";
        List<String> roles = List.of("CUSTOMER", "ADMIN");

        String token = jwtTokenProvider.generateToken(userId, email, roles);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals(userId, jwtTokenProvider.getUserIdFromToken(token));
        assertEquals(email, jwtTokenProvider.getEmailFromToken(token));
        assertEquals(roles, jwtTokenProvider.getRolesFromToken(token));
    }

    @Test
    void generateAndValidateToken_WithoutEmail() {
        String userId = "user-123";
        List<String> roles = List.of("CUSTOMER");

        String token = jwtTokenProvider.generateToken(userId, roles);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals(userId, jwtTokenProvider.getUserIdFromToken(token));
        assertNull(jwtTokenProvider.getEmailFromToken(token));
        assertEquals(roles, jwtTokenProvider.getRolesFromToken(token));
    }

    @Test
    void validateToken_InvalidSignature() {
        String userId = "user-123";
        List<String> roles = List.of("CUSTOMER");
        String token = jwtTokenProvider.generateToken(userId, roles);

        // Mutate token slightly to break signature validation
        String badToken = token + "abc";

        assertFalse(jwtTokenProvider.validateToken(badToken));
    }

    @Test
    void validateToken_Malformed() {
        assertFalse(jwtTokenProvider.validateToken("not-a-jwt"));
    }

    @Test
    void getRolesFromToken_HandlesException() {
        // If roles claim is missing or malformed, should return empty list
        String badToken = "invalid";
        List<String> roles = jwtTokenProvider.getRolesFromToken(badToken);
        assertNotNull(roles);
        assertTrue(roles.isEmpty());
    }
}
