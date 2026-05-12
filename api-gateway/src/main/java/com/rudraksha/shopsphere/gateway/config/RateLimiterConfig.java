package com.rudraksha.shopsphere.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

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
    public KeyResolver principalNameKeyResolver() {
        return exchange -> {
            InetSocketAddress addr = exchange.getRequest().getRemoteAddress();
            String key = addr != null ? addr.getAddress().getHostAddress() : "unknown";
            return Mono.just(key);
        };
    }
}
