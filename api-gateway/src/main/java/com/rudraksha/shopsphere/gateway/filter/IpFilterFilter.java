package com.rudraksha.shopsphere.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
public class IpFilterFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(IpFilterFilter.class);

    @Value("${gateway.ip-whitelist:}")
    private String whitelist;

    @Value("${gateway.ip-blacklist:}")
    private String blacklist;

    @Value("${gateway.ip-filter-enabled:false}")
    private boolean filterEnabled;

    private Set<String> whitelistSet;
    private Set<String> blacklistSet;

    public IpFilterFilter() {
        this.whitelistSet = new HashSet<>();
        this.blacklistSet = new HashSet<>();
    }

    private void initializeFilters() {
        if (whitelist != null && !whitelist.isEmpty()) {
            whitelistSet = new HashSet<>(Arrays.asList(whitelist.split(",")));
            whitelistSet.forEach(ip -> whitelistSet.add(ip.trim()));
        }
        if (blacklist != null && !blacklist.isEmpty()) {
            blacklistSet = new HashSet<>(Arrays.asList(blacklist.split(",")));
            blacklistSet.forEach(ip -> blacklistSet.add(ip.trim()));
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!filterEnabled) {
            return chain.filter(exchange);
        }

        initializeFilters();

        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress == null) {
            logger.warn("Unable to determine client IP address");
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        String clientIp = remoteAddress.getAddress().getHostAddress();

        if (!whitelistSet.isEmpty() && !whitelistSet.contains(clientIp)) {
            logger.warn("IP {} not in whitelist", clientIp);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        if (!blacklistSet.isEmpty() && blacklistSet.contains(clientIp)) {
            logger.warn("IP {} is blacklisted", clientIp);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }
}
