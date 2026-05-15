package com.rudraksha.shopsphere.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SecurityAlertService {

    public void alertTokenReuse(String userId, String oldToken, String attemptedToken) {
        log.error("CRITICAL SECURITY ALERT: Refresh token reuse detected for user: {}. Old token: {}, Attempted token: {}",
                userId, oldToken, attemptedToken);
        // In a real system, this would send an email to the user, trigger a PagerDuty alert, 
        // and potentially lock the user account.
    }
}
