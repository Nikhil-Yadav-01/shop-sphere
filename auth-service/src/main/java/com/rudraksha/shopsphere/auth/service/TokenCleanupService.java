package com.rudraksha.shopsphere.auth.service;

import com.rudraksha.shopsphere.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 0 * * ?") // Run at midnight every day
    @Transactional
    public void cleanupTokens() {
        log.info("Starting scheduled token cleanup...");
        try {
            refreshTokenRepository.deleteExpiredOrRevokedTokens();
            log.info("Scheduled token cleanup completed successfully.");
        } catch (Exception e) {
            log.error("Error during scheduled token cleanup", e);
        }
    }
}
