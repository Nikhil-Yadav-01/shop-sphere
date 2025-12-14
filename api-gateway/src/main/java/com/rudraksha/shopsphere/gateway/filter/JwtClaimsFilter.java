package com.rudraksha.shopsphere.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class JwtClaimsFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(JwtClaimsFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${jwt.secret:your-secret-key-change-in-production}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            try {
                String token = authHeader.substring(BEARER_PREFIX.length());
                Claims claims = parseJwt(token);

                String userId = claims.getSubject();
                String roles = (String) claims.get("roles");
                String permissions = (String) claims.get("permissions");

                exchange.getRequest().mutate()
                        .header("X-User-Id", userId)
                        .header("X-User-Roles", roles != null ? roles : "")
                        .header("X-User-Permissions", permissions != null ? permissions : "")
                        .build();

                logger.debug("JWT claims extracted - UserId: {}, Roles: {}", userId, roles);
            } catch (Exception e) {
                logger.debug("Failed to parse JWT claims: {}", e.getMessage());
                // Continue processing without claims if parsing fails
            }
        }

        return chain.filter(exchange);
    }

    private Claims parseJwt(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
