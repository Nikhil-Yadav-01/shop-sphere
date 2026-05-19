package com.rudraksha.shopsphere.auth.service.impl;

import com.rudraksha.shopsphere.auth.dto.SocialUserInfo;
import com.rudraksha.shopsphere.auth.dto.request.*;
import com.rudraksha.shopsphere.auth.dto.response.AuthResponse;
import com.rudraksha.shopsphere.auth.dto.response.TokenResponse;
import com.rudraksha.shopsphere.auth.dto.response.TokenValidationResponse;
import com.rudraksha.shopsphere.auth.entity.EmailVerificationToken;
import com.rudraksha.shopsphere.auth.entity.PasswordResetToken;
import com.rudraksha.shopsphere.auth.entity.RefreshToken;
import com.rudraksha.shopsphere.auth.entity.User;
import com.rudraksha.shopsphere.auth.exception.AuthException;
import com.rudraksha.shopsphere.auth.exception.UserAlreadyExistsException;
import com.rudraksha.shopsphere.auth.kafka.UserEventPublisher;
import com.rudraksha.shopsphere.auth.repository.EmailVerificationTokenRepository;
import com.rudraksha.shopsphere.auth.repository.PasswordResetTokenRepository;
import com.rudraksha.shopsphere.auth.repository.RefreshTokenRepository;
import com.rudraksha.shopsphere.auth.repository.UserRepository;
import com.rudraksha.shopsphere.auth.service.*;
import com.rudraksha.shopsphere.shared.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    private UserEventPublisher userEventPublisher;

    @Mock
    private EmailService emailService;

    @Mock
    private TokenRevocationService tokenRevocationService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private RateLimitingService rateLimitingService;

    @Mock
    private SecurityAlertService securityAlertService;

    @Mock
    private GoogleAuthService googleAuthService;

    @Mock
    private AppleAuthService appleAuthService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User localUser;
    private User disabledUser;
    private User googleUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtExpirationMs", 3600000L);
        ReflectionTestUtils.setField(authService, "refreshExpirationMs", 604800000L);
        ReflectionTestUtils.setField(authService, "verificationTokenExpiryMinutes", 15);
        ReflectionTestUtils.setField(authService, "autoVerify", false);

        localUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .password("encoded_password")
                .firstName("John")
                .lastName("Doe")
                .role(User.Role.CUSTOMER)
                .authProvider(User.AuthProvider.LOCAL)
                .enabled(true)
                .emailVerified(true)
                .build();

        disabledUser = User.builder()
                .id(UUID.randomUUID())
                .email("disabled@example.com")
                .password("encoded_password")
                .firstName("Disabled")
                .lastName("User")
                .role(User.Role.CUSTOMER)
                .authProvider(User.AuthProvider.LOCAL)
                .enabled(false)
                .emailVerified(false)
                .build();

        googleUser = User.builder()
                .id(UUID.randomUUID())
                .email("google@example.com")
                .firstName("Google")
                .lastName("User")
                .role(User.Role.CUSTOMER)
                .authProvider(User.AuthProvider.GOOGLE)
                .providerId("google123")
                .enabled(true)
                .emailVerified(true)
                .build();
    }

    // ==========================================
    // LOGIN TESTS
    // ==========================================

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        
        when(rateLimitingService.isLoginAllowed(request.getEmail())).thenReturn(true);
        doNothing().when(loginAttemptService).checkLockout(request.getEmail());
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(localUser));
        when(passwordEncoder.matches(request.getPassword(), localUser.getPassword())).thenReturn(true);
        when(jwtTokenProvider.generateToken(anyString(), anyString(), anyList())).thenReturn("access_token");
        
        RefreshToken refreshToken = RefreshToken.builder()
                .token("refresh_token")
                .user(localUser)
                .build();
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertEquals("CUSTOMER", response.getRole());

        verify(loginAttemptService).loginSucceeded(request.getEmail());
        verify(rateLimitingService).resetLoginAttempts(request.getEmail());
    }

    @Test
    void login_RateLimitExceeded() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        when(rateLimitingService.isLoginAllowed(request.getEmail())).thenReturn(false);

        assertThrows(AuthException.class, () -> authService.login(request));
        verifyNoInteractions(userRepository);
    }

    @Test
    void login_AccountLocked() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        when(rateLimitingService.isLoginAllowed(request.getEmail())).thenReturn(true);
        doThrow(new AuthException("Account is locked")).when(loginAttemptService).checkLockout(request.getEmail());

        assertThrows(AuthException.class, () -> authService.login(request));
        verifyNoInteractions(userRepository);
    }

    @Test
    void login_UserNotFound() {
        LoginRequest request = new LoginRequest("nonexistent@example.com", "password123");
        when(rateLimitingService.isLoginAllowed(request.getEmail())).thenReturn(true);
        doNothing().when(loginAttemptService).checkLockout(request.getEmail());
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        assertThrows(AuthException.class, () -> authService.login(request));
        verify(loginAttemptService).loginFailed(request.getEmail());
    }

    @Test
    void login_WrongProvider() {
        LoginRequest request = new LoginRequest("google@example.com", "password123");
        when(rateLimitingService.isLoginAllowed(request.getEmail())).thenReturn(true);
        doNothing().when(loginAttemptService).checkLockout(request.getEmail());
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(googleUser));

        AuthException exception = assertThrows(AuthException.class, () -> authService.login(request));
        assertTrue(exception.getMessage().contains("Please use GOOGLE login"));
    }

    @Test
    void login_WrongPassword() {
        LoginRequest request = new LoginRequest("test@example.com", "wrong_password");
        when(rateLimitingService.isLoginAllowed(request.getEmail())).thenReturn(true);
        doNothing().when(loginAttemptService).checkLockout(request.getEmail());
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(localUser));
        when(passwordEncoder.matches(request.getPassword(), localUser.getPassword())).thenReturn(false);

        assertThrows(AuthException.class, () -> authService.login(request));
        verify(loginAttemptService).loginFailed(request.getEmail());
    }

    @Test
    void login_AccountDisabled() {
        LoginRequest request = new LoginRequest("disabled@example.com", "password123");
        when(rateLimitingService.isLoginAllowed(request.getEmail())).thenReturn(true);
        doNothing().when(loginAttemptService).checkLockout(request.getEmail());
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(disabledUser));
        when(passwordEncoder.matches(request.getPassword(), disabledUser.getPassword())).thenReturn(true);

        AuthException exception = assertThrows(AuthException.class, () -> authService.login(request));
        assertEquals("Account is disabled", exception.getMessage());
    }

    // ==========================================
    // REGISTRATION TESTS
    // ==========================================

    @Test
    void register_Success_RequiresVerification() {
        RegisterRequest request = new RegisterRequest("new@example.com", "password123", "Jane", "Smith");
        when(rateLimitingService.isRegisterAllowed(request.getEmail())).thenReturn(true);
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded_new_password");
        
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        RefreshToken refreshToken = RefreshToken.builder()
                .token("refresh_token")
                .user(localUser)
                .build();
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);
        when(jwtTokenProvider.generateToken(anyString(), anyString(), anyList())).thenReturn("access_token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
        verify(userEventPublisher).publishUserCreatedEvent(any(User.class));
        verify(emailService).sendVerificationEmail(eq("new@example.com"), anyString(), anyString());
    }

    @Test
    void register_Success_AutoVerify() {
        ReflectionTestUtils.setField(authService, "autoVerify", true);
        RegisterRequest request = new RegisterRequest("new@example.com", "password123", "Jane", "Smith");
        when(rateLimitingService.isRegisterAllowed(request.getEmail())).thenReturn(true);
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded_new_password");
        
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        RefreshToken refreshToken = RefreshToken.builder()
                .token("refresh_token")
                .user(localUser)
                .build();
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);
        when(jwtTokenProvider.generateToken(anyString(), anyString(), anyList())).thenReturn("access_token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        verifyNoInteractions(emailVerificationTokenRepository);
        verify(userEventPublisher).publishUserCreatedEvent(any(User.class));
    }

    @Test
    void register_RateLimitExceeded() {
        RegisterRequest request = new RegisterRequest("new@example.com", "password123", "Jane", "Smith");
        when(rateLimitingService.isRegisterAllowed(request.getEmail())).thenReturn(false);

        assertThrows(AuthException.class, () -> authService.register(request));
        verifyNoInteractions(userRepository);
    }

    @Test
    void register_EmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "Jane", "Smith");
        when(rateLimitingService.isRegisterAllowed(request.getEmail())).thenReturn(true);
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
    }

    // ==========================================
    // SOCIAL LOGIN TESTS
    // ==========================================

    @Test
    void googleLogin_Success_ExistingUser() {
        GoogleLoginRequest request = new GoogleLoginRequest("google_id_token");
        SocialUserInfo socialInfo = SocialUserInfo.builder()
                .providerId("google123")
                .email("google@example.com")
                .firstName("Google")
                .lastName("User")
                .build();
        
        when(googleAuthService.verify(request.getIdToken())).thenReturn(socialInfo);
        when(userRepository.findByAuthProviderAndProviderId(User.AuthProvider.GOOGLE, "google123"))
                .thenReturn(Optional.of(googleUser));
        
        when(jwtTokenProvider.generateToken(anyString(), anyString(), anyList())).thenReturn("access_token");
        RefreshToken refreshToken = RefreshToken.builder().token("refresh_token").user(googleUser).build();
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        AuthResponse response = authService.googleLogin(request);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("google@example.com", response.getEmail());
    }

    @Test
    void googleLogin_Success_LinkAccount() {
        GoogleLoginRequest request = new GoogleLoginRequest("google_id_token");
        SocialUserInfo socialInfo = SocialUserInfo.builder()
                .providerId("google123")
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();
        
        when(googleAuthService.verify(request.getIdToken())).thenReturn(socialInfo);
        when(userRepository.findByAuthProviderAndProviderId(User.AuthProvider.GOOGLE, "google123"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(localUser));
        when(userRepository.save(any(User.class))).thenReturn(localUser);

        when(jwtTokenProvider.generateToken(anyString(), anyString(), anyList())).thenReturn("access_token");
        RefreshToken refreshToken = RefreshToken.builder().token("refresh_token").user(localUser).build();
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        AuthResponse response = authService.googleLogin(request);

        assertNotNull(response);
        assertEquals(User.AuthProvider.GOOGLE, localUser.getAuthProvider());
        assertEquals("google123", localUser.getProviderId());
        assertTrue(localUser.isEmailVerified());
    }

    @Test
    void googleLogin_Success_CreateNewUser() {
        GoogleLoginRequest request = new GoogleLoginRequest("google_id_token");
        SocialUserInfo socialInfo = SocialUserInfo.builder()
                .providerId("google_new")
                .email("new_social@example.com")
                .firstName("New")
                .lastName("User")
                .build();
        
        when(googleAuthService.verify(request.getIdToken())).thenReturn(socialInfo);
        when(userRepository.findByAuthProviderAndProviderId(User.AuthProvider.GOOGLE, "google_new"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("new_social@example.com")).thenReturn(Optional.empty());
        
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        when(jwtTokenProvider.generateToken(anyString(), anyString(), anyList())).thenReturn("access_token");
        RefreshToken refreshToken = RefreshToken.builder().token("refresh_token").user(localUser).build();
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        AuthResponse response = authService.googleLogin(request);

        assertNotNull(response);
        verify(userEventPublisher).publishUserCreatedEvent(any(User.class));
    }

    @Test
    void appleLogin_Success_NewUserWithRequestNames() {
        AppleLoginRequest request = new AppleLoginRequest("apple_id_token", "AppleFirst", "AppleLast");
        SocialUserInfo socialInfo = SocialUserInfo.builder()
                .providerId("apple123")
                .email("apple@example.com")
                .firstName(null)
                .lastName(null)
                .build();
        
        when(appleAuthService.verify(request.getIdToken())).thenReturn(socialInfo);
        when(userRepository.findByAuthProviderAndProviderId(User.AuthProvider.APPLE, "apple123"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("apple@example.com")).thenReturn(Optional.empty());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        when(jwtTokenProvider.generateToken(anyString(), anyString(), anyList())).thenReturn("access_token");
        RefreshToken refreshToken = RefreshToken.builder().token("refresh_token").user(localUser).build();
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        authService.appleLogin(request);

        User savedUser = userCaptor.getValue();
        assertEquals("AppleFirst", savedUser.getFirstName());
        assertEquals("AppleLast", savedUser.getLastName());
        assertEquals(User.AuthProvider.APPLE, savedUser.getAuthProvider());
    }

    // ==========================================
    // REFRESH TOKEN TESTS
    // ==========================================

    @Test
    void refreshToken_Success() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid_refresh_token");
        RefreshToken token = RefreshToken.builder()
                .token("valid_refresh_token")
                .user(localUser)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(false)
                .build();
        
        when(refreshTokenRepository.findByToken(request.getRefreshToken())).thenReturn(Optional.of(token));
        when(jwtTokenProvider.generateToken(anyString(), anyString(), anyList())).thenReturn("new_access_token");
        
        RefreshToken newRefreshToken = RefreshToken.builder().token("new_refresh_token").build();
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(newRefreshToken);

        TokenResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new_access_token", response.getAccessToken());
        assertEquals("new_refresh_token", response.getRefreshToken());
        assertTrue(token.isRevoked());
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void refreshToken_NotFound() {
        RefreshTokenRequest request = new RefreshTokenRequest("nonexistent_token");
        when(refreshTokenRepository.findByToken(request.getRefreshToken())).thenReturn(Optional.empty());

        assertThrows(AuthException.class, () -> authService.refreshToken(request));
    }

    @Test
    void refreshToken_Expired() {
        RefreshTokenRequest request = new RefreshTokenRequest("expired_refresh_token");
        RefreshToken token = RefreshToken.builder()
                .token("expired_refresh_token")
                .user(localUser)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .revoked(false)
                .build();
        
        when(refreshTokenRepository.findByToken(request.getRefreshToken())).thenReturn(Optional.of(token));

        AuthException exception = assertThrows(AuthException.class, () -> authService.refreshToken(request));
        assertEquals("Refresh token is expired or revoked", exception.getMessage());
    }

    @Test
    void refreshToken_Revoked_TriggersReuseAlert() {
        RefreshTokenRequest request = new RefreshTokenRequest("revoked_refresh_token");
        RefreshToken token = RefreshToken.builder()
                .token("revoked_refresh_token")
                .user(localUser)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(true)
                .build();
        
        when(refreshTokenRepository.findByToken(request.getRefreshToken())).thenReturn(Optional.of(token));

        assertThrows(AuthException.class, () -> authService.refreshToken(request));
        verify(securityAlertService).alertTokenReuse(anyString(), eq("revoked_refresh_token"), eq("revoked_refresh_token"));
    }

    // ==========================================
    // LOGOUT & VALIDATION TESTS
    // ==========================================

    @Test
    void logout_Success() {
        String token = "access_token";
        RefreshToken rt = RefreshToken.builder().token(token).revoked(false).build();
        
        when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.of(rt));

        authService.logout(token);

        verify(tokenRevocationService).revokeToken(token, 3600L);
        assertTrue(rt.isRevoked());
        verify(refreshTokenRepository).save(rt);
    }

    @Test
    void validateToken_Success() {
        String token = "valid_token";
        when(tokenRevocationService.isTokenRevoked(token)).thenReturn(false);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn("user_id");
        when(jwtTokenProvider.getRolesFromToken(token)).thenReturn(List.of("CUSTOMER"));

        TokenValidationResponse response = authService.validateToken(token);

        assertNotNull(response);
        assertTrue(response.isValid());
        assertEquals("user_id", response.getUserId());
        assertEquals(List.of("CUSTOMER"), response.getRoles());
    }

    @Test
    void validateToken_Revoked() {
        String token = "revoked_token";
        when(tokenRevocationService.isTokenRevoked(token)).thenReturn(true);

        TokenValidationResponse response = authService.validateToken(token);

        assertNotNull(response);
        assertFalse(response.isValid());
        assertNull(response.getUserId());
    }

    @Test
    void validateToken_Invalid() {
        String token = "invalid_token";
        when(tokenRevocationService.isTokenRevoked(token)).thenReturn(false);
        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        TokenValidationResponse response = authService.validateToken(token);

        assertNotNull(response);
        assertFalse(response.isValid());
    }

    @Test
    void validateToken_ThrowsException() {
        String token = "buggy_token";
        when(tokenRevocationService.isTokenRevoked(token)).thenThrow(new RuntimeException("Redis error"));

        TokenValidationResponse response = authService.validateToken(token);

        assertNotNull(response);
        assertFalse(response.isValid());
    }

    // ==========================================
    // EMAIL VERIFICATION TESTS
    // ==========================================

    @Test
    void verifyEmail_Success() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .token("verify_token")
                .user(disabledUser)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .used(false)
                .build();
        
        when(emailVerificationTokenRepository.findByToken("verify_token")).thenReturn(Optional.of(token));

        authService.verifyEmail("verify_token");

        assertTrue(disabledUser.isEnabled());
        assertTrue(disabledUser.isEmailVerified());
        assertTrue(token.isUsed());
        verify(userRepository).save(disabledUser);
        verify(emailVerificationTokenRepository).save(token);
    }

    @Test
    void verifyEmail_Expired() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .token("expired_token")
                .user(disabledUser)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .used(false)
                .build();
        
        when(emailVerificationTokenRepository.findByToken("expired_token")).thenReturn(Optional.of(token));

        assertThrows(AuthException.class, () -> authService.verifyEmail("expired_token"));
    }

    @Test
    void resendVerificationEmail_Success() {
        disabledUser.setEmailVerified(false);
        when(userRepository.findByEmail("disabled@example.com")).thenReturn(Optional.of(disabledUser));

        authService.resendVerificationEmail("disabled@example.com");

        verify(emailVerificationTokenRepository).deleteByUser(disabledUser);
        verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
        verify(emailService).sendVerificationEmail(eq("disabled@example.com"), anyString(), anyString());
    }

    @Test
    void resendVerificationEmail_AlreadyVerified() {
        localUser.setEmailVerified(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(localUser));

        assertThrows(AuthException.class, () -> authService.resendVerificationEmail("test@example.com"));
    }

    // ==========================================
    // PASSWORD RESET TESTS
    // ==========================================

    @Test
    void forgotPassword_Success() {
        when(rateLimitingService.isForgotPasswordAllowed("test@example.com")).thenReturn(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(localUser));

        authService.forgotPassword("test@example.com");

        verify(passwordResetTokenRepository).deleteByUser(localUser);
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq("test@example.com"), anyString(), anyString());
    }

    @Test
    void forgotPassword_RateLimit() {
        when(rateLimitingService.isForgotPasswordAllowed("test@example.com")).thenReturn(false);

        assertThrows(AuthException.class, () -> authService.forgotPassword("test@example.com"));
    }

    @Test
    void resetPassword_Success() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("reset_token")
                .user(localUser)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .used(false)
                .build();
        
        when(passwordResetTokenRepository.findByToken("reset_token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("new_password")).thenReturn("new_encoded_password");

        authService.resetPassword("reset_token", "new_password");

        assertEquals("new_encoded_password", localUser.getPassword());
        assertTrue(token.isUsed());
        verify(userRepository).save(localUser);
        verify(passwordResetTokenRepository).save(token);
    }
}
