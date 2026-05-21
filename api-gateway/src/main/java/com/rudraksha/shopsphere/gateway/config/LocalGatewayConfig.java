package com.rudraksha.shopsphere.gateway.config;

import com.rudraksha.shopsphere.gateway.filter.AuthenticationFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalGatewayConfig {

    private final AuthenticationFilter authenticationFilter;

    public LocalGatewayConfig(AuthenticationFilter authenticationFilter) {
        this.authenticationFilter = authenticationFilter;
    }

    @Bean
    public RouteLocator localRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r
                        .path("/auth/**")
                        .uri("http://localhost:8081"))

                .route("user-service", r -> r
                        .path("/users/**")
                        .filters(f -> f.filter(
                                authenticationFilter.apply(new AuthenticationFilter.Config())))
                        .uri("http://localhost:8082"))

                .route("catalog-service", r -> r
                        .path("/catalog/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("http://localhost:8083"))

                .route("admin-service", r -> r
                        .path("/admin/**")
                        .filters(f -> f.filter(
                                authenticationFilter.apply(new AuthenticationFilter.Config(java.util.List.of("ADMIN")))))
                        .uri("http://localhost:8089"))

                .route("cart-service", r -> r
                        .path("/cart/**")
                        .filters(f -> f.stripPrefix(1)
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
                        .uri("http://localhost:8085"))

                .route("checkout-service", r -> r
                        .path("/checkout/**")
                        .filters(f -> f.stripPrefix(1)
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
                        .uri("http://localhost:8086"))

                .route("inventory-service", r -> r
                        .path("/inventory/**")
                        .filters(f -> f.stripPrefix(1)
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
                        .uri("http://localhost:8092"))

                .route("payment-service", r -> r
                        .path("/payment/**")
                        .filters(f -> f.stripPrefix(1)
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
                        .uri("http://localhost:8093"))

                .route("order-service", r -> r
                        .path("/order/**")
                        .filters(f -> f.stripPrefix(1)
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
                        .uri("http://localhost:8084"))

                .route("notification-service", r -> r
                        .path("/notification/**")
                        .filters(f -> f.stripPrefix(1)
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
                        .uri("http://localhost:8095"))

                .route("fraud-service", r -> r
                        .path("/fraud/**")
                        .filters(f -> f.stripPrefix(1)
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config(java.util.List.of("ADMIN")))))
                        .uri("http://localhost:8010"))

                .route("pricing-service", r -> r
                        .path("/pricing/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("http://localhost:8087"))

                .route("coupon-service", r -> r
                        .path("/coupons/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("http://localhost:8088"))

                .route("review-service", r -> r
                        .path("/reviews/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("http://localhost:8096"))

                .route("media-service", r -> r
                        .path("/media/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("http://localhost:8091"))

                .route("analytics-service", r -> r
                        .path("/analytics/**")
                        .filters(f -> f.stripPrefix(1)
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config(java.util.List.of("ADMIN")))))
                        .uri("http://localhost:8090"))

                .route("search-service", r -> r
                        .path("/search/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("http://localhost:8099"))

                .route("returns-service", r -> r
                        .path("/returns/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("http://localhost:8097"))

                .route("recommendation-service", r -> r
                        .path("/recommendations/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("http://localhost:8098"))

                .route("batch-service", r -> r
                        .path("/batch/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("http://localhost:8094"))

                .build();
    }
}
