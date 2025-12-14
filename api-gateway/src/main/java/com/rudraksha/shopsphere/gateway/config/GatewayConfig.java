package com.rudraksha.shopsphere.gateway.config;

import com.rudraksha.shopsphere.gateway.filter.AuthenticationFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    private final AuthenticationFilter authenticationFilter;
    private final GatewayRouteProperties routeProperties;

    public GatewayConfig(AuthenticationFilter authenticationFilter, GatewayRouteProperties routeProperties) {
        this.authenticationFilter = authenticationFilter;
        this.routeProperties = routeProperties;
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
                        .filters(f -> f.filter(authenticationFilter.apply(new Object())))
                        .uri("lb://admin-service"))
                .route("cart-service", r -> r
                        .path("/cart/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://cart-service"))
                .route("checkout-service", r -> r
                        .path("/checkout/**")
                        .filters(f -> f.stripPrefix(1).filter(authenticationFilter.apply(new Object())))
                        .uri("lb://checkout-service"))
                .route("order-service", r -> r
                        .path("/order/**")
                        .filters(f -> f.stripPrefix(1).filter(authenticationFilter.apply(new Object())))
                        .uri("lb://order-service"))
                .route("pricing-service", r -> r
                        .path("/pricing/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://pricing-service"))
                .route("user-service", r -> r
                        .path("/user/**")
                        .filters(f -> f.stripPrefix(1).filter(authenticationFilter.apply(new Object())))
                        .uri("lb://user-service"))
                .route("review-service", r -> r
                        .path("/review/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://review-service"))
                .route("search-service", r -> r
                        .path("/search/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://search-service"))
                .route("recommendation-service", r -> r
                        .path("/recommendation/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://recommendation-service"))
                .route("notification-service", r -> r
                        .path("/notification/**")
                        .filters(f -> f.stripPrefix(1).filter(authenticationFilter.apply(new Object())))
                        .uri("lb://notification-service"))
                .route("returns-service", r -> r
                        .path("/returns/**")
                        .filters(f -> f.stripPrefix(1).filter(authenticationFilter.apply(new Object())))
                        .uri("lb://returns-service"))
                .route("media-service", r -> r
                        .path("/media/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://media-service"))
                .build();
    }
}
