package com.rudraksha.shopsphere.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<Object> {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);
    private static final String CACHE_KEY_PREFIX = "jwt:";
    private static final long JWT_CACHE_TTL_MINUTES = 30;

    private final WebClient webClient;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${auth.service.url}")
    private String authServiceUrl;

    @Value("${auth.request.timeout:5000}")
    private long requestTimeoutMs;

    public AuthenticationFilter(WebClient.Builder webClientBuilder,
                                RedisTemplate<String, String> redisTemplate) {
        this.webClient = webClientBuilder.build();
        this.redisTemplate = redisTemplate;
    }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            String token = exchange.getRequest()
                    .getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            if (token == null || !token.startsWith("Bearer ")) {
                logger.warn("Missing or invalid authorization header");
                return sendUnauthorized(exchange);
            }

            String cacheKey = CACHE_KEY_PREFIX + token;
            String cachedValidation = redisTemplate.opsForValue().get(cacheKey);

            if ("valid".equals(cachedValidation)) {
                logger.debug("JWT validated from cache");
                return chain.filter(exchange);
            }

            return validateTokenWithRetry(token)
                    .flatMap(response -> {
                        redisTemplate.opsForValue().set(cacheKey, "valid", JWT_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                        logger.info("JWT validated and cached");
                        return chain.filter(exchange);
                    })
                    .onErrorResume(error -> {
                        logger.warn("JWT validation failed: {}", error.getMessage());
                        redisTemplate.opsForValue().set(cacheKey, "invalid", 5, TimeUnit.MINUTES);
                        return sendUnauthorized(exchange);
                    });
        };
    }

    private Mono<Void> validateTokenWithRetry(String token) {
        return webClient.post()
                .uri(authServiceUrl + "/auth/validate")
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .onStatus(status -> status.isError(),
                        response -> Mono.error(new RuntimeException("JWT validation failed")))
                .toBodilessEntity()
                .timeout(Duration.ofMillis(requestTimeoutMs))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(100))
                        .maxBackoff(Duration.ofSeconds(1))
                        .filter(throwable -> !(throwable instanceof RuntimeException)))
                .then();
    }

    private Mono<Void> sendUnauthorized(org.springframework.web.server.ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
