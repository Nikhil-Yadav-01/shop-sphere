package com.rudraksha.shopsphere.gateway.filter;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class MetricsFilter implements GlobalFilter, Ordered {

    private final MeterRegistry meterRegistry;

    public MetricsFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();
        String method = exchange.getRequest().getMethod().toString();
        String metricName = "gateway.request." + method.toLowerCase() + "." + sanitizePath(path);

        Timer.Sample sample = Timer.start(meterRegistry);

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    int statusCode = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value()
                            : 500;

                    sample.stop(Timer.builder(metricName)
                            .tag("method", method)
                            .tag("path", path)
                            .tag("status", String.valueOf(statusCode))
                            .publishPercentiles(0.5, 0.95, 0.99)
                            .register(meterRegistry));

                    meterRegistry.counter("gateway.requests.total",
                            "method", method,
                            "path", path,
                            "status", String.valueOf(statusCode)).increment();
                });
    }

    private String sanitizePath(String path) {
        return path.replaceAll("[^a-zA-Z0-9]", "_").replaceAll("_+", "_");
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
