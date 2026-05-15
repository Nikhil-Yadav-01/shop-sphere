package com.rudraksha.shopsphere.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:3600000}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String userId, List<String> roles) {
        return generateToken(userId, null, roles);
    }

    public String generateToken(String userId, String email, List<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        var builder = Jwts.builder()
                .subject(userId)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey);

        if (email != null) {
            builder.claim("email", email);
        }

        return builder.compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public String getUserIdFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public String getEmailFromToken(String token) {
        return getClaims(token).get("email", String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        try {
            List<String> roles = getClaims(token).get("roles", List.class);
            return roles != null ? roles : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

