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
                
                // Catalog Service - Public routes
                .route("catalog-service", r -> r
                        .path("/catalog/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://CATALOG-SERVICE"))
                
                // Admin Service - Protected routes (requires ADMIN role)
                .route("admin-service", r -> r
                        .path("/admin/**")
                        .filters(f -> f.filter(authenticationFilter.apply(new Object())))
                        .uri("lb://admin-service"))
                
                // Batch Service - Public routes
                .route("batch-service", r -> r
                        .path("/batch/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://BATCH-SERVICE"))
                
                // Cart Service - Public routes
                .route("cart-service", r -> r
                        .path("/cart/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://CART-SERVICE"))
                
                // Checkout Service - Protected routes (requires authentication)
                .route("checkout-service", r -> r
                        .path("/checkout/**")
                        .filters(f -> f.stripPrefix(1).filter(authenticationFilter.apply(new Object())))
                        .uri("lb://CHECKOUT-SERVICE"))
                
                // Pricing Service - Public routes
                .route("pricing-service", r -> r
                        .path("/pricing/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://pricing-service"))
                
                // Order Service - Protected routes (requires authentication)
                .route("order-service", r -> r
                        .path("/order/**")
                        .filters(f -> f.stripPrefix(1).filter(authenticationFilter.apply(new Object())))
                        .uri("lb://order-service"))
                
                .build();
    }
}
