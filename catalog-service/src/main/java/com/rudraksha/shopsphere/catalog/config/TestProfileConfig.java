package com.rudraksha.shopsphere.catalog.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
@ComponentScan(
    basePackages = "com.rudraksha.shopsphere.catalog",
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*Repository.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*ServiceImpl.*"),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
            com.rudraksha.shopsphere.catalog.controller.ProductController.class,
            com.rudraksha.shopsphere.catalog.controller.CategoryController.class
        })
    }
)
public class TestProfileConfig {
}