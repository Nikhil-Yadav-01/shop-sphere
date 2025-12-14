package com.rudraksha.shopsphere.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CorsFilter implements GlobalFilter, Ordered {

    @Value("${cors.allowed-origins:*}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS,PATCH}")
    private String allowedMethods;

    @Value("${cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${cors.exposed-headers:X-Correlation-ID,X-Trace-ID,X-Rate-Limit-Remaining,X-Rate-Limit-Reset}")
    private String exposedHeaders;

    @Value("${cors.max-age:3600}")
    private String maxAge;

    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();

        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            var response = exchange.getResponse();
            setCommonCorsHeaders(response);
            response.setStatusCode(HttpStatus.OK);
            return response.setComplete();
        }

        return chain.filter(exchange)
                .doOnSuccess(v -> setCommonCorsHeaders(exchange.getResponse()));
    }

    private void setCommonCorsHeaders(org.springframework.http.server.reactive.ServerHttpResponse response) {
        response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigins);
        response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, allowedMethods);
        response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, allowedHeaders);
        response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, exposedHeaders);
        response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_MAX_AGE, maxAge);
        
        if (allowCredentials) {
            response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE - 1;
    }
}
