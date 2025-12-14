package com.rudraksha.shopsphere.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class RoleBasedAccessFilter extends AbstractGatewayFilterFactory<RoleBasedAccessFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(RoleBasedAccessFilter.class);

    public RoleBasedAccessFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (config.requiredRoles == null || config.requiredRoles.length == 0) {
                return chain.filter(exchange);
            }

            String userRolesHeader = exchange.getRequest().getHeaders().getFirst("X-User-Roles");
            
            if (userRolesHeader == null || userRolesHeader.isEmpty()) {
                logger.warn("No user roles found in request headers");
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            List<String> userRoles = Arrays.asList(userRolesHeader.split(","));
            boolean hasRequiredRole = Arrays.stream(config.requiredRoles)
                    .anyMatch(userRoles::contains);

            if (!hasRequiredRole) {
                logger.warn("User does not have required roles. Required: {}, User has: {}", 
                           Arrays.toString(config.requiredRoles), userRolesHeader);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            return chain.filter(exchange);
        };
    }

    public static class Config {
        private String[] requiredRoles;

        public String[] getRequiredRoles() {
            return requiredRoles;
        }

        public void setRequiredRoles(String[] requiredRoles) {
            this.requiredRoles = requiredRoles;
        }
    }
}
