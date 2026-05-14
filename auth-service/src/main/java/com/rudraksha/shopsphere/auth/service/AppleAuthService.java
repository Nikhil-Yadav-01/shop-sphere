package com.rudraksha.shopsphere.auth.service;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.rudraksha.shopsphere.auth.dto.SocialUserInfo;
import com.rudraksha.shopsphere.auth.exception.AuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;

@Slf4j
@Service
public class AppleAuthService {

    private final String clientId;
    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
    private static final String APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    public AppleAuthService(@Value("${oauth2.apple.client-id}") String clientId) throws Exception {
        this.clientId = clientId;
        this.jwtProcessor = new DefaultJWTProcessor<>();
        
        // Use RemoteJWKSet which handles caching and rotation
        JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(
                new URL(APPLE_JWKS_URL),
                new DefaultResourceRetriever(5000, 5000)
        );
        
        JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(
                com.nimbusds.jose.JWSAlgorithm.RS256, keySource);
        this.jwtProcessor.setJWSKeySelector(keySelector);
    }

    public SocialUserInfo verify(String idTokenString) {
        try {
            JWTClaimsSet claimsSet = jwtProcessor.process(idTokenString, null);

            if (!claimsSet.getIssuer().equals(APPLE_ISSUER)) {
                log.warn("Invalid Apple token issuer: {}", claimsSet.getIssuer());
                throw new AuthException("Invalid Apple token issuer");
            }

            if (!claimsSet.getAudience().contains(clientId)) {
                log.warn("Invalid Apple token audience: {}", claimsSet.getAudience());
                throw new AuthException("Invalid Apple token audience");
            }

            return SocialUserInfo.builder()
                    .email(claimsSet.getStringClaim("email"))
                    .providerId(claimsSet.getSubject())
                    .build();
        } catch (Exception e) {
            log.error("Error verifying Apple ID token", e);
            throw new AuthException("Failed to verify Apple ID token");
        }
    }
}
