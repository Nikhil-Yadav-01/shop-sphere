package com.rudraksha.shopsphere.shared.db;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(FlywayProperties.class)
public class FlywayConfig {

    @Bean
    public Flyway flyway(DataSource dataSource, FlywayProperties flywayProperties) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(flywayProperties.getLocations().toArray(new String[0]))
                .baselineOnMigrate(flywayProperties.isBaselineOnMigrate())
                .baselineVersion(flywayProperties.getBaselineVersion())
                .validateOnMigrate(flywayProperties.isValidateOnMigrate())
                .outOfOrder(flywayProperties.isOutOfOrder())
                .cleanDisabled(true)
                .load();
    }
}
