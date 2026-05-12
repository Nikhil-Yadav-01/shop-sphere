package com.rudraksha.shopsphere.gateway.filter;

import com.rudraksha.shopsphere.gateway.dto.TokenValidationResponse;
import com.rudraksha.shopsphere.gateway.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final JwtTokenProvider jwtTokenProvider;
    private final WebClient webClient;
    private final CacheManager cacheManager;

    public AuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            @Qualifier("loadBalancedWebClientBuilder") WebClient.Builder webClientBuilder,
            @Value("${auth.service.url}") String authServiceUrl,
            CacheManager cacheManager) {
        super(Config.class);
        this.jwtTokenProvider = jwtTokenProvider;
        this.webClient = webClientBuilder.baseUrl(authServiceUrl).build();
        this.cacheManager = cacheManager;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest()
                    .getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED,
                        "MISSING_TOKEN", "Authorization header is required");
            }

            String token = authHeader.substring(7);

            if (!jwtTokenProvider.validateToken(token)) {
                return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED,
                        "INVALID_TOKEN", "Token is invalid or expired");
            }

            String userId = jwtTokenProvider.getUserIdFromToken(token);
            List<String> roles = jwtTokenProvider.getRolesFromToken(token);

            return checkRevocation(token)
                    .flatMap(revocationResult -> {
                        if (revocationResult.revoked()) {
                            return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED,
                                    "TOKEN_REVOKED", "Token has been revoked");
                        }

                        if (!config.requiredRoles.isEmpty()) {
                            boolean hasRequiredRole = config.requiredRoles.stream()
                                    .anyMatch(required -> roles.stream()
                                            .anyMatch(r -> r.equalsIgnoreCase(required)));
                            if (!hasRequiredRole) {
                                return writeErrorResponse(exchange, HttpStatus.FORBIDDEN,
                                        "FORBIDDEN", "Insufficient permissions");
                            }
                        }

                        ServerWebExchange mutatedExchange = exchange.mutate()
                                .request(r -> r
                                        .header("X-User-Id", userId)
                                        .header("X-User-Roles", String.join(",", roles)))
                                .build();

                        return chain.filter(mutatedExchange);
                    })
                    .onErrorResume(e -> {
                        if (!exchange.getResponse().isCommitted()) {
                            return writeErrorResponse(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                                    "AUTH_UNAVAILABLE", "Authentication service is unavailable");
                        }
                        return Mono.empty();
                    });
        };
    }

    private Mono<RevocationResult> checkRevocation(String token) {
        Cache cache = cacheManager.getCache("tokenValidation");
        String cacheKey = "revoked:" + token.hashCode();

        if (cache != null) {
            Cache.ValueWrapper cached = cache.get(cacheKey);
            if (cached != null) {
                return Mono.just(new RevocationResult((Boolean) cached.get()));
            }
        }

        return webClient.post()
                .uri("/auth/validate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(TokenValidationResponse.class)
                .map(response -> {
                    boolean revoked = !response.isValid();
                    if (cache != null) {
                        cache.put(cacheKey, revoked);
                    }
                    return new RevocationResult(revoked);
                });
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus status,
                                          String error, String message) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.empty();
        }
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format(
                "{\"error\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                error, message, Instant.now().toString());
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    public static class Config {
        private List<String> requiredRoles;

        public Config() {
            this.requiredRoles = List.of();
        }

        public Config(List<String> requiredRoles) {
            this.requiredRoles = requiredRoles;
        }

        public List<String> getRequiredRoles() {
            return requiredRoles;
        }

        public void setRequiredRoles(List<String> requiredRoles) {
            this.requiredRoles = requiredRoles;
        }
    }

    private record RevocationResult(boolean revoked) {}
}
