package com.rudraksha.shopsphere.gateway.config;

import com.rudraksha.shopsphere.gateway.filter.AuthenticationFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
public class LocalGatewayConfig {

    private final AuthenticationFilter authenticationFilter;

    public LocalGatewayConfig(AuthenticationFilter authenticationFilter) {
        this.authenticationFilter = authenticationFilter;
    }

    @Bean
    @Primary
    public RouteLocator localRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth Service - Direct URL for local testing
                .route("auth-service", r -> r
                        .path("/auth/**")
                        .uri("http://localhost:8081"))
                
                // User Service - Protected routes
                .route("user-service", r -> r
                        .path("/users/**")
                        .filters(f -> f.filter(authenticationFilter.apply(new Object())))
                        .uri("http://localhost:8082"))
                
                .build();
    }
}