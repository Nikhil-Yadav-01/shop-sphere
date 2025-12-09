package com.rudraksha.shopsphere.shared.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auto-configuration for Security-related beans.
 * This ensures JwtTokenProvider and JwtAuthenticationFilter are available
 * in all services that depend on common-security.
 */
@AutoConfiguration
@ComponentScan(basePackages = "com.rudraksha.shopsphere.shared.security")
public class SecurityAutoConfiguration {

}
