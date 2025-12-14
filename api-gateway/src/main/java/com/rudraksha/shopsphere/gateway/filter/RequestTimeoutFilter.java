package com.rudraksha.shopsphere.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class RequestTimeoutFilter extends AbstractGatewayFilterFactory<RequestTimeoutFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(RequestTimeoutFilter.class);

    @Value("${gateway.request.timeout:30000}")
    private long defaultTimeoutMs;

    public RequestTimeoutFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        long timeoutMs = config.getTimeoutMs() > 0 ? config.getTimeoutMs() : defaultTimeoutMs;

        return (exchange, chain) -> {
            return chain.filter(exchange)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .onErrorResume(error -> {
                        if (error instanceof org.reactivestreams.Publisher) {
                            logger.error("Request timeout after {}ms", timeoutMs);
                            exchange.getResponse().setStatusCode(HttpStatus.GATEWAY_TIMEOUT);
                            return exchange.getResponse().setComplete();
                        }
                        throw error instanceof RuntimeException ? (RuntimeException) error : new RuntimeException(error);
                    });
        };
    }

    public static class Config {
        private long timeoutMs = 0;

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }
}
