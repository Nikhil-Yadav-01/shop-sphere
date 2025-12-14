package com.rudraksha.shopsphere.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CsrfProtectionFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(CsrfProtectionFilter.class);
    private static final String CSRF_TOKEN_HEADER = "X-CSRF-Token";
    private static final String CSRF_TOKEN_COOKIE = "csrf-token";

    @Value("${csrf.protection-enabled:true}")
    private boolean csrfProtectionEnabled;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!csrfProtectionEnabled) {
            return chain.filter(exchange);
        }

        var request = exchange.getRequest();
        var method = request.getMethod();

        // Generate CSRF token for GET requests (to be used in subsequent requests)
        if (HttpMethod.GET.equals(method)) {
            String csrfToken = UUID.randomUUID().toString();
            exchange.getResponse().getHeaders().add(CSRF_TOKEN_HEADER, csrfToken);
            exchange.getResponse().getCookies().add(CSRF_TOKEN_COOKIE,
                    org.springframework.http.ResponseCookie.from(CSRF_TOKEN_COOKIE, csrfToken)
                            .secure(true)
                            .httpOnly(true)
                            .path("/")
                            .maxAge(3600)
                            .build());
            return chain.filter(exchange);
        }

        // Validate CSRF token for state-changing requests
        if (isStateChangingRequest(method)) {
            String csrfTokenHeader = request.getHeaders().getFirst(CSRF_TOKEN_HEADER);
            
            if (csrfTokenHeader == null || csrfTokenHeader.isEmpty()) {
                logger.warn("CSRF token missing in {} request to {}", method, request.getPath());
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            // In production, validate token against session/database
            // For now, just validate presence
        }

        return chain.filter(exchange);
    }

    private boolean isStateChangingRequest(HttpMethod method) {
        return HttpMethod.POST.equals(method) ||
               HttpMethod.PUT.equals(method) ||
               HttpMethod.DELETE.equals(method) ||
               HttpMethod.PATCH.equals(method);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 7;
    }
}
