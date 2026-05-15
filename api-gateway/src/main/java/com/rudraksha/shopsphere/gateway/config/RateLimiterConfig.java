package com.rudraksha.shopsphere.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Optional;

@Configuration
@Profile("!local")
public class RateLimiterConfig {

    @Value("${rate-limiter.replenish-rate:100}")
    private int replenishRate;

    @Value("${rate-limiter.burst-capacity:200}")
    private int burstCapacity;

    @Value("${rate-limiter.requested-tokens:1}")
    private int requestedTokens;

    @Bean
    public RedisRateLimiter defaultRateLimiter() {
        return new RedisRateLimiter(replenishRate, burstCapacity, requestedTokens);
    }

    @Bean
    public RedisRateLimiter catalogRateLimiter() {
        // Search and browse need higher limits
        return new RedisRateLimiter(200, 400, 1);
    }

    @Bean
    public RedisRateLimiter authRateLimiter() {
        // Login and token endpoints should be tighter to prevent brute force
        return new RedisRateLimiter(10, 20, 1);
    }

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            InetSocketAddress addr = exchange.getRequest().getRemoteAddress();
            String key = addr != null && addr.getAddress() != null ? 
                addr.getAddress().getHostAddress() : "unknown";
            return Mono.just(key);
        };
    }

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.justOrEmpty(
                exchange.getRequest().getHeaders().getFirst("X-User-Id"))
                .switchIfEmpty(Mono.defer(() -> {
                    String host = Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                            .map(addr -> addr.getAddress() != null ? addr.getAddress().getHostAddress() : addr.getHostString())
                            .orElse("anonymous");
                    return Mono.just(host);
                }));
    }

    @Bean
    public KeyResolver principalNameKeyResolver() {
        return ipKeyResolver();
    }
}
