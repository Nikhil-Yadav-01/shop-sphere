package com.rudraksha.shopsphere.coupon.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = "com.rudraksha.shopsphere.coupon.repository")
@EnableTransactionManagement
public class DatabaseConfig {
}