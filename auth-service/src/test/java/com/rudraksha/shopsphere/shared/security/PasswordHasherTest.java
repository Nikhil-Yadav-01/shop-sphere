package com.rudraksha.shopsphere.shared.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    private final PasswordHasher passwordHasher = new PasswordHasher();

    @Test
    void hashAndMatches_Success() {
        String password = "my_secure_password";
        String hash = passwordHasher.hash(password);

        assertNotNull(hash);
        assertNotEquals(password, hash);
        assertTrue(passwordHasher.matches(password, hash));
        assertFalse(passwordHasher.matches("wrong_password", hash));
    }
}
