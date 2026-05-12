package com.rudraksha.shopsphere.gateway.config;

import com.rudraksha.shopsphere.gateway.filter.AuthenticationFilter;
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

    public GatewayConfig(AuthenticationFilter authenticationFilter) {
        this.authenticationFilter = authenticationFilter;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r
                        .path("/auth/**")
                        .uri("lb://auth-service"))

                .route("catalog-service", r -> r
                        .path("/catalog/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://catalog-service"))

                .route("admin-service", r -> r
                        .path("/admin/**")
                        .filters(f -> f.filter(
                                authenticationFilter.apply(
                                        new AuthenticationFilter.Config(List.of("ADMIN")))))
                        .uri("lb://admin-service"))

                .route("batch-service", r -> r
                        .path("/batch/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://batch-service"))

                .route("cart-service", r -> r
                        .path("/cart/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://cart-service"))

                .route("checkout-service", r -> r
                        .path("/checkout/**")
                        .filters(f -> f.stripPrefix(1)
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
                        .uri("lb://checkout-service"))

                .route("pricing-service", r -> r
                        .path("/pricing/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://pricing-service"))

                .route("order-service", r -> r
                        .path("/order/**")
                        .filters(f -> f.stripPrefix(1)
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
                        .uri("lb://order-service"))

                .build();
    }
}
