package com.rudraksha.shopsphere.gateway.config;

import com.rudraksha.shopsphere.gateway.filter.AuthenticationFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    private final AuthenticationFilter authenticationFilter;

    public GatewayConfig(AuthenticationFilter authenticationFilter) {
        this.authenticationFilter = authenticationFilter;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth Service - Public routes
                .route("auth-service", r -> r
                        .path("/auth/**")
                        .uri("lb://auth-service"))
                
                // Admin Service - Protected routes (requires ADMIN role)
                .route("admin-service", r -> r
                        .path("/admin/**")
                        .filters(f -> f.filter(authenticationFilter.apply(new Object())))
                        .uri("lb://admin-service"))
                
                .build();
    }
}
