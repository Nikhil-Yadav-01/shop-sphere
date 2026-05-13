package com.rudraksha.shopsphere.auth.service.impl;

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
import com.rudraksha.shopsphere.auth.service.AuthService;
import com.rudraksha.shopsphere.auth.service.EmailService;
import com.rudraksha.shopsphere.auth.service.LoginAttemptService;
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
        
        loginAttemptService.checkLockout(request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed - user not found: {}", request.getEmail());
                    loginAttemptService.loginFailed(request.getEmail());
                    return new AuthException("Invalid email or password");
                });

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

        String accessToken = generateAccessToken(user);
        RefreshToken refreshToken = createRefreshToken(user);

        log.info("User logged in successfully: {}", request.getEmail());
        return buildAuthResponse(user, accessToken, refreshToken.getToken());
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.debug("Registration attempt for email: {}", request.getEmail());

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
            emailService.sendVerificationEmail(user.getEmail(), verificationToken);
        }

        userEventPublisher.publishUserCreatedEvent(user);

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

        emailService.sendVerificationEmail(user.getEmail(), verificationToken);
        log.info("Verification email resent to: {}", email);
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
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

        emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
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
