package com.rudraksha.shopsphere.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;

/**
 * Routes requests based on HTTP method:
 * - GET requests to public routes (no auth needed)
 * - POST/PUT/DELETE requests to protected routes (auth required)
 * 
 * This allows catalog service to have both public reads and protected writes.
 */
@Component
public class MethodBasedRoutingFilter implements GlobalFilter, Ordered {
    
    private static final Logger logger = LoggerFactory.getLogger(MethodBasedRoutingFilter.class);
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        HttpMethod method = exchange.getRequest().getMethod();
        
        // Check if this is a catalog service request
        if (path.startsWith("/catalog/")) {
            // GET requests are public (read operations)
            if (method == HttpMethod.GET) {
                logger.debug("Catalog GET request - public access allowed: {}", path);
                return chain.filter(exchange);
            }
            
            // POST/PUT/DELETE/PATCH require authentication (write operations)
            if (method == HttpMethod.POST || method == HttpMethod.PUT || 
                method == HttpMethod.DELETE || method == HttpMethod.PATCH) {
                
                // Check for JWT token
                String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    logger.warn("Catalog {} request without JWT - rejecting: {}", method, path);
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
                
                logger.debug("Catalog {} request with JWT - authentication required: {}", method, path);
                // Continue to authentication filter
            }
        }
        
        return chain.filter(exchange);
    }
    
    @Override
    public int getOrder() {
        // Execute after correlation ID but before authentication
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
