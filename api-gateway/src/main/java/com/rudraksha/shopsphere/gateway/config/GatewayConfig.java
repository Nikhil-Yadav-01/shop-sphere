package com.rudraksha.shopsphere.gateway.config;

import com.rudraksha.shopsphere.gateway.filter.AuthenticationFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
@Profile("!local")
public class GatewayConfig {

    private final AuthenticationFilter authenticationFilter;
    private final KeyResolver userKeyResolver;
    private final KeyResolver ipKeyResolver;
    private final RedisRateLimiter defaultRateLimiter;
    private final RedisRateLimiter catalogRateLimiter;
    private final RedisRateLimiter authRateLimiter;

    public GatewayConfig(
            AuthenticationFilter authenticationFilter,
            @Qualifier("userKeyResolver") KeyResolver userKeyResolver,
            @Qualifier("ipKeyResolver") KeyResolver ipKeyResolver,
            @Qualifier("defaultRateLimiter") RedisRateLimiter defaultRateLimiter,
            @Qualifier("catalogRateLimiter") RedisRateLimiter catalogRateLimiter,
            @Qualifier("authRateLimiter") RedisRateLimiter authRateLimiter) {
        this.authenticationFilter = authenticationFilter;
        this.userKeyResolver = userKeyResolver;
        this.ipKeyResolver = ipKeyResolver;
        this.defaultRateLimiter = defaultRateLimiter;
        this.catalogRateLimiter = catalogRateLimiter;
        this.authRateLimiter = authRateLimiter;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r
                        .path("/auth/**")
                        .filters(f -> f.requestRateLimiter(config -> {
                            config.setRateLimiter(authRateLimiter);
                            config.setKeyResolver(ipKeyResolver);
                        }))
                        .uri("lb://auth-service"))

                .route("catalog-service", r -> r
                        .path("/catalog/**")
                        .filters(f -> f.stripPrefix(1)
                                .requestRateLimiter(config -> {
                                    config.setRateLimiter(catalogRateLimiter);
                                    config.setKeyResolver(userKeyResolver);
                                }))
                        .uri("lb://catalog-service"))

                .route("admin-service", r -> r
                        .path("/admin/**")
                        .filters(f -> f.filter(
                                authenticationFilter.apply(
                                        new AuthenticationFilter.Config(List.of("ADMIN"))))
                                .requestRateLimiter(config -> {
                                    config.setRateLimiter(defaultRateLimiter);
                                    config.setKeyResolver(userKeyResolver);
                                }))
                        .uri("lb://admin-service"))

                .route("batch-service", r -> r
                        .path("/batch/**")
                        .filters(f -> f.stripPrefix(1)
                                .requestRateLimiter(config -> {
                                    config.setRateLimiter(defaultRateLimiter);
                                    config.setKeyResolver(userKeyResolver);
                                }))
                        .uri("lb://batch-service"))

                .route("cart-service", r -> r
                        .path("/cart/**")
                        .filters(f -> f.stripPrefix(1)
                                .requestRateLimiter(config -> {
                                    config.setRateLimiter(defaultRateLimiter);
                                    config.setKeyResolver(userKeyResolver);
                                }))
                        .uri("lb://cart-service"))

                .route("checkout-service", r -> r
                        .path("/checkout/**")
                        .filters(f -> f.stripPrefix(1)
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                                .requestRateLimiter(config -> {
                                    config.setRateLimiter(defaultRateLimiter);
                                    config.setKeyResolver(userKeyResolver);
                                }))
                        .uri("lb://checkout-service"))

                .route("pricing-service", r -> r
                        .path("/pricing/**")
                        .filters(f -> f.stripPrefix(1)
                                .requestRateLimiter(config -> {
                                    config.setRateLimiter(defaultRateLimiter);
                                    config.setKeyResolver(userKeyResolver);
                                }))
                        .uri("lb://pricing-service"))

                .build();
    }
}
