package com.rudraksha.shopsphere.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for per-service request timeouts.
 * Prevents slow services from blocking the entire gateway.
 */
@Configuration
@ConfigurationProperties(prefix = "gateway")
public class TimeoutConfig {
    
    private Map<String, Long> serviceTimeouts = new HashMap<>();

    public Map<String, Long> getServiceTimeouts() {
        return serviceTimeouts;
    }

    public void setServiceTimeouts(Map<String, Long> serviceTimeouts) {
        this.serviceTimeouts = serviceTimeouts;
    }

    /**
     * Get timeout for a specific service in milliseconds.
     * Falls back to default 30s if not configured.
     */
    public long getTimeoutForService(String serviceName) {
        String normalizedName = serviceName.toLowerCase().replace("_", "-");
        return serviceTimeouts.getOrDefault(normalizedName, 30000L);
    }
}
