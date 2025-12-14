package com.rudraksha.shopsphere.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        String method = exchange.getRequest().getMethod().toString();
        String path = exchange.getRequest().getPath().toString();
        String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-ID");
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");

        logger.info("Incoming request - Method: {}, Path: {}, CorrelationId: {}, UserId: {}", 
                   method, path, correlationId, userId);

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    long duration = System.currentTimeMillis() - startTime;
                    int statusCode = exchange.getResponse().getStatusCode() != null 
                        ? exchange.getResponse().getStatusCode().value() 
                        : 500;

                    logger.info("Request completed - Method: {}, Path: {}, Status: {}, Duration: {}ms, CorrelationId: {}", 
                               method, path, statusCode, duration, correlationId);

                    if (statusCode >= 400) {
                        logger.warn("Error response - Method: {}, Path: {}, Status: {}, CorrelationId: {}", 
                                   method, path, statusCode, correlationId);
                    }
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
