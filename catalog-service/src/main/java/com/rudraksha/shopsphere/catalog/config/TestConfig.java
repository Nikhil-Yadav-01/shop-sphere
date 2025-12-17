package com.rudraksha.shopsphere.catalog.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@ConditionalOnProperty(name = "spring.data.mongodb.repositories.enabled", havingValue = "true", matchIfMissing = true)
@EnableMongoRepositories(basePackages = "com.rudraksha.shopsphere.catalog.repository")
public class TestConfig {
}