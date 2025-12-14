package com.rudraksha.shopsphere.gateway.filter;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    private static final String CORRELATION_ID_MDC = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString();
        }

        final String finalCorrelationId = correlationId;
        final String finalTraceId = traceId;

        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, finalCorrelationId);
        exchange.getResponse().getHeaders().add(TRACE_ID_HEADER, finalTraceId);

        return chain.filter(exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .header(CORRELATION_ID_HEADER, finalCorrelationId)
                        .header(TRACE_ID_HEADER, finalTraceId)
                        .build())
                .build())
                .contextWrite(ctx -> ctx.put(CORRELATION_ID_MDC, finalCorrelationId));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
