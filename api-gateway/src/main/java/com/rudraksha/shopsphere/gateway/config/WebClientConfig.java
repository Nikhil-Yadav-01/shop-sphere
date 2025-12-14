package com.rudraksha.shopsphere.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("custom")
                .maxConnections(500)
                .maxIdleTime(Duration.ofSeconds(30))
                .maxLifeTime(Duration.ofMinutes(30))
                .pendingAcquireTimeout(Duration.ofSeconds(45))
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .responseTimeout(Duration.ofSeconds(10));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
