package com.rudraksha.shopsphere.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OAuth2ClientConfig {

    @Bean
    public OAuth2ClientProperties oAuth2ClientProperties() {
        return new OAuth2ClientProperties();
    }

    public static class OAuth2ClientProperties {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        private String[] scopes;

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }

        public String[] getScopes() {
            return scopes;
        }

        public void setScopes(String[] scopes) {
            this.scopes = scopes;
        }
    }
}
