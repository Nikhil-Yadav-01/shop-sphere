package com.rudraksha.shopsphere.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties(MtlsProperties.class)
@ConditionalOnProperty(prefix = "mtls", name = "enabled", havingValue = "true")
public class MtlsConfig {

    @Bean
    public WebClient mtlsWebClient(MtlsProperties mtlsProperties) {
        HttpClient httpClient = buildMtlsHttpClient(mtlsProperties);
        return WebClient.builder()
                .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(httpClient))
                .build();
    }

    private HttpClient buildMtlsHttpClient(MtlsProperties props) {
        // HttpClient uses reactor-netty which requires io.netty.handler.ssl.SslContext
        // For basic mTLS support, we rely on system SSL properties
        // In production, use spring-cloud-vault or similar for certificate management
        return HttpClient.create()
                .secure(sslSpec -> {
                    // Default system SSL context will be used
                });
    }
}

@ConfigurationProperties(prefix = "mtls")
class MtlsProperties {
    private boolean enabled;
    private String keyStorePath;
    private String keyStorePassword;
    private String trustStorePath;
    private String trustStorePassword;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public void setKeyStorePath(String keyStorePath) {
        this.keyStorePath = keyStorePath;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getTrustStorePath() {
        return trustStorePath;
    }

    public void setTrustStorePath(String trustStorePath) {
        this.trustStorePath = trustStorePath;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }
}
