package com.rudraksha.shopsphere.auth.service.impl;

import com.rudraksha.shopsphere.auth.dto.SocialUserInfo;
import com.rudraksha.shopsphere.auth.dto.request.AppleLoginRequest;
import com.rudraksha.shopsphere.auth.dto.request.GoogleLoginRequest;
import com.rudraksha.shopsphere.auth.dto.request.LoginRequest;
import com.rudraksha.shopsphere.auth.dto.request.RefreshTokenRequest;
import com.rudraksha.shopsphere.auth.dto.request.RegisterRequest;
import com.rudraksha.shopsphere.auth.dto.response.AuthResponse;
import com.rudraksha.shopsphere.auth.dto.response.TokenResponse;
import com.rudraksha.shopsphere.auth.dto.response.TokenValidationResponse;
import com.rudraksha.shopsphere.auth.entity.RefreshToken;
import com.rudraksha.shopsphere.auth.entity.User;
import com.rudraksha.shopsphere.auth.exception.AuthException;
import com.rudraksha.shopsphere.auth.exception.UserAlreadyExistsException;
import com.rudraksha.shopsphere.auth.kafka.UserEventPublisher;
import com.rudraksha.shopsphere.auth.repository.EmailVerificationTokenRepository;
import com.rudraksha.shopsphere.auth.repository.PasswordResetTokenRepository;
import com.rudraksha.shopsphere.auth.repository.RefreshTokenRepository;
import com.rudraksha.shopsphere.auth.repository.UserRepository;
import com.rudraksha.shopsphere.auth.service.AppleAuthService;
import com.rudraksha.shopsphere.auth.service.AuthService;
import com.rudraksha.shopsphere.auth.service.EmailService;
import com.rudraksha.shopsphere.auth.service.GoogleAuthService;
import com.rudraksha.shopsphere.auth.service.LoginAttemptService;
import com.rudraksha.shopsphere.auth.service.RateLimitingService;
import com.rudraksha.shopsphere.auth.service.SecurityAlertService;
import com.rudraksha.shopsphere.auth.service.TokenRevocationService;
import com.rudraksha.shopsphere.auth.entity.EmailVerificationToken;
import com.rudraksha.shopsphere.auth.entity.PasswordResetToken;
import com.rudraksha.shopsphere.shared.security.JwtTokenProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserEventPublisher userEventPublisher;
    private final EmailService emailService;
    private final TokenRevocationService tokenRevocationService;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginAttemptService loginAttemptService;
    private final RateLimitingService rateLimitingService;
    private final SecurityAlertService securityAlertService;
    private final GoogleAuthService googleAuthService;
    private final AppleAuthService appleAuthService;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Value("${app.verification.token-expiry-minutes}")
    private int verificationTokenExpiryMinutes;

    @Value("${app.verification.auto-verify:false}")
    private boolean autoVerify;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.debug("Login attempt for email: {}", request.getEmail());
        
        if (!rateLimitingService.isLoginAllowed(request.getEmail())) {
            throw new AuthException("Too many login attempts. Please try again later.");
        }
        
        loginAttemptService.checkLockout(request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed - user not found: {}", request.getEmail());
                    loginAttemptService.loginFailed(request.getEmail());
                    return new AuthException("Invalid email or password");
                });

        if (user.getAuthProvider() != User.AuthProvider.LOCAL) {
            log.warn("Login failed - user is registered with {}: {}", user.getAuthProvider(), request.getEmail());
            throw new AuthException("Please use " + user.getAuthProvider() + " login");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed - invalid password for user: {}", request.getEmail());
            loginAttemptService.loginFailed(request.getEmail());
            throw new AuthException("Invalid email or password");
        }

        if (!user.isEnabled()) {
            log.warn("Login failed - account disabled for user: {}", request.getEmail());
            throw new AuthException("Account is disabled");
        }
        
        loginAttemptService.loginSucceeded(request.getEmail());
        rateLimitingService.resetLoginAttempts(request.getEmail());

        String accessToken = generateAccessToken(user);
        RefreshToken refreshToken = createRefreshToken(user);

        log.info("User logged in successfully: {}", request.getEmail());
        return buildAuthResponse(user, accessToken, refreshToken.getToken());
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.debug("Registration attempt for email: {}", request.getEmail());

        if (!rateLimitingService.isRegisterAllowed(request.getEmail())) {
            throw new AuthException("Too many registration attempts. Please try again later.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed - email already exists: {}", request.getEmail());
            throw new UserAlreadyExistsException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(User.Role.CUSTOMER)
                .authProvider(User.AuthProvider.LOCAL)
                .enabled(autoVerify)
                .emailVerified(autoVerify)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", request.getEmail());

        if (!autoVerify) {
            String verificationToken = UUID.randomUUID().toString();
            EmailVerificationToken emailToken = EmailVerificationToken.builder()
                    .token(verificationToken)
                    .user(user)
                    .expiresAt(LocalDateTime.now().plusMinutes(verificationTokenExpiryMinutes))
                    .used(false)
                    .build();
            emailVerificationTokenRepository.save(emailToken);

            log.info("[{}] VERIFICATION_TOKEN: {}", user.getEmail(), verificationToken);
            publishEmailEvent(user.getEmail(), "Verify your email", 
                    "Please verify your email using token: " + verificationToken);
        }

        userEventPublisher.publishUserCreatedEvent(user);

        String accessToken = generateAccessToken(user);
        RefreshToken refreshToken = createRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken.getToken());
    }

    private void publishEmailEvent(String to, String subject, String body) {
        try {
            com.rudraksha.shopsphere.auth.dto.EmailNotificationEvent event = 
                com.rudraksha.shopsphere.auth.dto.EmailNotificationEvent.builder()
                    .to(to)
                    .subject(subject)
                    .body(body)
                    .build();
            kafkaTemplate.send("notification.email.send", to, event);
            log.info("Published email notification event for: {}", to);
        } catch (Exception e) {
            log.error("Failed to publish email notification event for: {}", to, e);
            // Fallback to sync email if kafka fails or handle appropriately
        }
    }

    @Override
    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        log.debug("Google login attempt");
        SocialUserInfo userInfo = googleAuthService.verify(request.getIdToken());
        return processSocialLogin(userInfo, User.AuthProvider.GOOGLE);
    }

    @Override
    @Transactional
    public AuthResponse appleLogin(AppleLoginRequest request) {
        log.debug("Apple login attempt");
        SocialUserInfo userInfo = appleAuthService.verify(request.getIdToken());
        
        // Apple only sends name on the first login, so we use it from the request if present
        if (request.getFirstName() != null) userInfo.setFirstName(request.getFirstName());
        if (request.getLastName() != null) userInfo.setLastName(request.getLastName());
        
        return processSocialLogin(userInfo, User.AuthProvider.APPLE);
    }

    private AuthResponse processSocialLogin(SocialUserInfo userInfo, User.AuthProvider provider) {
        Optional<User> userOptional = userRepository.findByAuthProviderAndProviderId(provider, userInfo.getProviderId());
        
        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            log.info("{} user logged in: {}", provider, user.getEmail());
        } else {
            // Check if user exists with the same email
            userOptional = userRepository.findByEmail(userInfo.getEmail());
            if (userOptional.isPresent()) {
                user = userOptional.get();
                // Link the account
                user.setAuthProvider(provider);
                user.setProviderId(userInfo.getProviderId());
                user.setEmailVerified(true); // Social email is verified
                user = userRepository.save(user);
                log.info("Linked {} account for existing user: {}", provider, user.getEmail());
            } else {
                // Create new user
                user = User.builder()
                        .email(userInfo.getEmail())
                        .firstName(userInfo.getFirstName() != null ? userInfo.getFirstName() : "User")
                        .lastName(userInfo.getLastName() != null ? userInfo.getLastName() : "")
                        .role(User.Role.CUSTOMER)
                        .authProvider(provider)
                        .providerId(userInfo.getProviderId())
                        .enabled(true)
                        .emailVerified(true)
                        .build();
                user = userRepository.save(user);
                log.info("Created new user via {}: {}", provider, user.getEmail());
                userEventPublisher.publishUserCreatedEvent(user);
            }
        }

        if (!user.isEnabled()) {
            throw new AuthException("Account is disabled");
        }

        String accessToken = generateAccessToken(user);
        RefreshToken refreshToken = createRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken.getToken());
    }

    @Override
    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        log.debug("Refresh token request received");

        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> {
                    log.warn("Refresh token failed - token not found");
                    return new AuthException("Invalid refresh token");
                });

        if (!refreshToken.isValid()) {
            if (refreshToken.isRevoked()) {
                securityAlertService.alertTokenReuse(refreshToken.getUser().getId().toString(), 
                        refreshToken.getToken(), request.getRefreshToken());
            }
            log.warn("Refresh token failed - token is expired or revoked");
            throw new AuthException("Refresh token is expired or revoked");
        }

        User user = refreshToken.getUser();
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        String newAccessToken = generateAccessToken(user);
        RefreshToken newRefreshToken = createRefreshToken(user);

        log.info("Token refreshed successfully for user: {}", user.getEmail());

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs / 1000)
                .build();
    }

    @Override
    @Transactional
    public void logout(String token) {
        log.debug("Logout request received");

        tokenRevocationService.revokeToken(token, jwtExpirationMs / 1000);

        refreshTokenRepository.findByToken(token)
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });

        log.info("User logged out successfully");
    }

    @Override
    public TokenValidationResponse validateToken(String token) {
        try {
            if (tokenRevocationService.isTokenRevoked(token)) {
                log.warn("Token validation failed: token is revoked");
                return new TokenValidationResponse(false, null, null);
            }

            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("Token validation failed: invalid token");
                return new TokenValidationResponse(false, null, null);
            }

            String userId = jwtTokenProvider.getUserIdFromToken(token);
            List<String> roles = jwtTokenProvider.getRolesFromToken(token);

            return new TokenValidationResponse(true, userId, roles);
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return new TokenValidationResponse(false, null, null);
        }
    }

    private String generateAccessToken(User user) {
        return jwtTokenProvider.generateToken(
                user.getId().toString(),
                user.getEmail(),
                List.of(user.getRole().name())
        );
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new AuthException("Invalid verification token"));

        if (!verificationToken.isValid()) {
            throw new AuthException("Verification token is expired or already used");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        user.setEnabled(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        emailVerificationTokenRepository.save(verificationToken);

        log.info("Email verified successfully for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("User not found"));

        if (user.isEmailVerified()) {
            throw new AuthException("Email is already verified");
        }

        emailVerificationTokenRepository.deleteByUser(user);

        String verificationToken = UUID.randomUUID().toString();
        EmailVerificationToken emailToken = EmailVerificationToken.builder()
                .token(verificationToken)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(verificationTokenExpiryMinutes))
                .used(false)
                .build();
        emailVerificationTokenRepository.save(emailToken);

        publishEmailEvent(user.getEmail(), "Verify your email", 
                "Please verify your email using token: " + verificationToken);
        log.info("Verification email resent to: {}", email);
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        if (!rateLimitingService.isForgotPasswordAllowed(email)) {
            throw new AuthException("Too many password reset requests. Please try again later.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("User not found"));

        passwordResetTokenRepository.deleteByUser(user);

        String resetToken = UUID.randomUUID().toString();
        PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                .token(resetToken)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(verificationTokenExpiryMinutes))
                .used(false)
                .build();
        passwordResetTokenRepository.save(passwordResetToken);

        publishEmailEvent(user.getEmail(), "Reset your password", 
                "Please reset your password using token: " + resetToken);
        log.info("Password reset email sent to: {}", email);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new AuthException("Invalid reset token"));

        if (!resetToken.isValid()) {
            throw new AuthException("Reset token is expired or already used");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset successfully for user: {}", user.getEmail());
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs / 1000)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .build();
    }
}
