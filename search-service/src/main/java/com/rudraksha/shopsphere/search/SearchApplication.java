package com.rudraksha.shopsphere.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableDiscoveryClient
@EnableMongoAuditing
public class SearchApplication {
    public static void main(String[] args) {
        SpringApplication.run(SearchApplication.class, args);
    }
}
