package com.rudraksha.shopsphere.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitingFilter extends AbstractGatewayFilterFactory<RateLimitingFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);
    
    private final RedisTemplate<String, String> redisTemplate;
    
    @Value("${rate-limiting.requests-per-minute:100}")
    private int requestsPerMinute;

    public RateLimitingFilter(RedisTemplate<String, String> redisTemplate) {
        super(Config.class);
        this.redisTemplate = redisTemplate;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String identifier = getClientIdentifier(exchange);
            String redisKey = "rate-limit:" + identifier;

            try {
                Long currentCount = redisTemplate.opsForValue().increment(redisKey);
                
                if (currentCount == 1) {
                    redisTemplate.expire(redisKey, 1, TimeUnit.MINUTES);
                }

                if (currentCount > requestsPerMinute) {
                    logger.warn("Rate limit exceeded for client: {}", identifier);
                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                    exchange.getResponse().getHeaders().add("X-Rate-Limit-Remaining", "0");
                    exchange.getResponse().getHeaders().add("X-Rate-Limit-Reset", String.valueOf(System.currentTimeMillis() + 60000));
                    return exchange.getResponse().setComplete();
                }

                // Add rate limit headers to RESPONSE (not request)
                exchange.getResponse().getHeaders().add("X-Rate-Limit-Remaining", String.valueOf(requestsPerMinute - currentCount));
                exchange.getResponse().getHeaders().add("X-Rate-Limit-Reset", String.valueOf(System.currentTimeMillis() + 60000));

                return chain.filter(exchange);
            } catch (Exception e) {
                logger.error("Error in rate limiting filter", e);
                // On Redis error, allow request to pass through
                return chain.filter(exchange);
            }
        };
    }

    private String getClientIdentifier(ServerWebExchange exchange) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (userId != null) {
            return "user:" + userId;
        }

        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null) {
            return "ip:" + remoteAddress.getAddress().getHostAddress();
        }

        return "unknown";
    }

    public static class Config {
    }
}
