package com.rudraksha.shopsphere.gateway.config;

import com.rudraksha.shopsphere.gateway.filter.AuthenticationFilter;
import com.rudraksha.shopsphere.gateway.filter.RoleBasedAccessFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    private final AuthenticationFilter authenticationFilter;
    private final RoleBasedAccessFilter roleBasedAccessFilter;
    private final GatewayRouteProperties routeProperties;

    public GatewayConfig(AuthenticationFilter authenticationFilter, 
                        RoleBasedAccessFilter roleBasedAccessFilter,
                        GatewayRouteProperties routeProperties) {
        this.authenticationFilter = authenticationFilter;
        this.roleBasedAccessFilter = roleBasedAccessFilter;
        this.routeProperties = routeProperties;
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
                        .uri("lb://catalog-service"))
                
                // Admin Service - Protected routes (requires ADMIN role)
                .route("admin-service", r -> r
                        .path("/admin/**")
                        .filters(f -> f.filter(authenticationFilter.apply(new Object()))
                                .filter(roleBasedAccessFilter.apply(roleConfig("ADMIN"))))
                        .uri("lb://admin-service"))
                
                // Cart Service - Public routes
                .route("cart-service", r -> r
                        .path("/cart/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://cart-service"))
                
                // Checkout Service - Protected routes (requires authentication)
                .route("checkout-service", r -> r
                        .path("/checkout/**")
                        .filters(f -> f.stripPrefix(1).filter(authenticationFilter.apply(new Object())))
                        .uri("lb://checkout-service"))
                
                // Order Service - Protected routes (requires authentication)
                .route("order-service", r -> r
                        .path("/order/**")
                        .filters(f -> f.stripPrefix(1).filter(authenticationFilter.apply(new Object())))
                        .uri("lb://order-service"))
                
                // Pricing Service - Public routes
                .route("pricing-service", r -> r
                        .path("/pricing/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://pricing-service"))
                
                // User Service - Protected routes
                .route("user-service", r -> r
                        .path("/user/**")
                        .filters(f -> f.stripPrefix(1).filter(authenticationFilter.apply(new Object())))
                        .uri("lb://user-service"))
                
                // Review Service - Public routes (read), protected (write)
                .route("review-service", r -> r
                        .path("/review/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://review-service"))
                
                // Search Service - Public routes
                .route("search-service", r -> r
                        .path("/search/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://search-service"))
                
                // Recommendation Service - Public routes
                .route("recommendation-service", r -> r
                        .path("/recommendation/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://recommendation-service"))
                
                // Notification Service - Protected routes
                .route("notification-service", r -> r
                        .path("/notification/**")
                        .filters(f -> f.stripPrefix(1).filter(authenticationFilter.apply(new Object())))
                        .uri("lb://notification-service"))
                
                // Returns Service - Protected routes
                .route("returns-service", r -> r
                        .path("/returns/**")
                        .filters(f -> f.stripPrefix(1).filter(authenticationFilter.apply(new Object())))
                        .uri("lb://returns-service"))
                
                // Media Service - Public routes (read), protected (write)
                .route("media-service", r -> r
                        .path("/media/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://media-service"))
                
                .build();
    }

    private RoleBasedAccessFilter.Config roleConfig(String... roles) {
        var config = new RoleBasedAccessFilter.Config();
        config.setRequiredRoles(roles);
        return config;
    }
}
