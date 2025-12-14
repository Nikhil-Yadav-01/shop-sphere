package com.rudraksha.shopsphere.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class HealthCheckFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckFilter.class);
    private final ConcurrentHashMap<String, Boolean> serviceHealthCache = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private DiscoveryClient discoveryClient;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String serviceName = exchange.getAttribute("serviceId");

        if (serviceName != null && !isServiceHealthy(serviceName)) {
            logger.warn("Service {} is unhealthy, rejecting request", serviceName);
            exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    private boolean isServiceHealthy(String serviceName) {
        if (!serviceHealthCache.containsKey(serviceName)) {
            boolean healthy = checkServiceHealth(serviceName);
            serviceHealthCache.put(serviceName, healthy);
            return healthy;
        }
        return serviceHealthCache.getOrDefault(serviceName, true);
    }

    private boolean checkServiceHealth(String serviceName) {
        if (discoveryClient == null) {
            return true;
        }

        try {
            var instances = discoveryClient.getInstances(serviceName);
            return !instances.isEmpty();
        } catch (Exception e) {
            logger.warn("Error checking health of service {}: {}", serviceName, e.getMessage());
            return false;
        }
    }

    public void invalidateCache(String serviceName) {
        serviceHealthCache.remove(serviceName);
    }

    public void invalidateAllCache() {
        serviceHealthCache.clear();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 3;
    }
}
