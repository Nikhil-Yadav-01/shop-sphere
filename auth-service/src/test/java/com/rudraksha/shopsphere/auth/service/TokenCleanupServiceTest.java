package com.rudraksha.shopsphere.auth.service;

import com.rudraksha.shopsphere.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenCleanupServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private TokenCleanupService tokenCleanupService;

    @Test
    void cleanupTokens_Success() {
        tokenCleanupService.cleanupTokens();

        verify(refreshTokenRepository).deleteExpiredOrRevokedTokens();
    }

    @Test
    void cleanupTokens_ExceptionHandled() {
        doThrow(new RuntimeException("DB down")).when(refreshTokenRepository).deleteExpiredOrRevokedTokens();

        // Should not propagate exception, just catch and log it
        tokenCleanupService.cleanupTokens();

        verify(refreshTokenRepository).deleteExpiredOrRevokedTokens();
    }
}
