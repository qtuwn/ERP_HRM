package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.AuthTokens;
import com.vthr.erp_hrm.core.model.AccountStatus;
import com.vthr.erp_hrm.core.model.Company;
import com.vthr.erp_hrm.core.model.EmailVerificationToken;
import com.vthr.erp_hrm.core.model.RefreshToken;
import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.model.User;
import com.vthr.erp_hrm.core.repository.EmailVerificationTokenRepository;
import com.vthr.erp_hrm.core.repository.PasswordResetTokenRepository;
import com.vthr.erp_hrm.core.repository.RefreshTokenRepository;
import com.vthr.erp_hrm.core.repository.UserRepository;
import com.vthr.erp_hrm.core.service.CompanyService;
import com.vthr.erp_hrm.infrastructure.email.EmailQueueService;
import com.vthr.erp_hrm.infrastructure.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailQueueService emailQueueService;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CompanyService companyService;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "otpExpirationMinutes", 10L);
        ReflectionTestUtils.setField(authService, "refreshExpirationMs", 60000L);
    }

    @Test
    void register_Candidate_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encoded_pass");
        
        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .fullName("Test User")
                .role(Role.CANDIDATE)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.hashToken(anyString())).thenReturn("hashed_token");

        User result = authService.register(
                "test@example.com", "password", "Test User", "123456789", 
                "CANDIDATE", null, null);

        assertNotNull(result);
        assertEquals(Role.CANDIDATE, result.getRole());
        verify(companyService, never()).createOrGetCompany(anyString());
        verify(emailQueueService).enqueueEmail(eq("test@example.com"), anyString(), anyString(), anyMap());
    }

    @Test
    void register_HR_Success() {
        when(userRepository.findByEmail("hr@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encoded");

        UUID companyId = UUID.randomUUID();
        Company mockCompany = Company.builder().id(companyId).name("Tech Corp").build();
        when(companyService.createOrGetCompany("Tech Corp")).thenReturn(mockCompany);

        User savedUser = User.builder().id(UUID.randomUUID()).email("hr@example.com").fullName("HR User").role(Role.HR).companyId(companyId).build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.hashToken(anyString())).thenReturn("hashed");

        User result = authService.register(
                "hr@example.com", "password", "HR User", "123456789", 
                "HR", "Tech Corp", "HR Dept");

        assertNotNull(result);
        assertEquals(companyId, result.getCompanyId());
        verify(companyService).createOrGetCompany("Tech Corp");
        verify(companyService).addHrMember(companyId, savedUser.getId(), "HR");
    }

    @Test
    void register_HR_MissingCompanyName_ThrowsException() {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            authService.register("hr@test.com", "pass", "Name", "123", "HR", "", null);
        });

        assertEquals("Company name is required for HR registration", exception.getMessage());
    }

    @Test
    void register_shouldRejectDuplicateEmail() {
        when(userRepository.findByEmail("dup@example.com"))
                .thenReturn(Optional.of(User.builder().id(UUID.randomUUID()).build()));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                authService.register("dup@example.com", "password", "Name", null, "CANDIDATE", null, null)
        );
        assertEquals("Email already exists", ex.getMessage());
    }

    @Test
    void verifyEmail_shouldRejectInvalidToken() {
        when(jwtService.hashToken("bad")).thenReturn("badHash");
        when(emailVerificationTokenRepository.findByTokenHash("badHash")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.verifyEmail("bad"));
        assertEquals("Invalid verification token", ex.getMessage());
    }

    @Test
    void login_shouldRejectWhenEmailNotVerified() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("u@example.com")
                .passwordHash("hash")
                .role(Role.CANDIDATE)
                .status(AccountStatus.PENDING)
                .isActive(true)
                .emailVerified(false)
                .build();

        when(userRepository.findByEmail("u@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "hash")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login("u@example.com", "pass"));
        assertEquals("Email is not verified", ex.getMessage());
    }

    @Test
    void login_shouldRejectWhenDisabled() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("u@example.com")
                .passwordHash("hash")
                .role(Role.CANDIDATE)
                .status(AccountStatus.ACTIVE)
                .isActive(false)
                .emailVerified(true)
                .build();

        when(userRepository.findByEmail("u@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "hash")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login("u@example.com", "pass"));
        assertEquals("User is disabled", ex.getMessage());
    }

    @Test
    void login_shouldRejectWhenSuspended() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("u@example.com")
                .passwordHash("hash")
                .role(Role.CANDIDATE)
                .status(AccountStatus.SUSPENDED)
                .isActive(true)
                .emailVerified(true)
                .build();

        when(userRepository.findByEmail("u@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "hash")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login("u@example.com", "pass"));
        assertEquals("User is suspended", ex.getMessage());
    }

    @Test
    void login_shouldRejectWrongPassword() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("u@example.com")
                .passwordHash("hash")
                .role(Role.CANDIDATE)
                .status(AccountStatus.ACTIVE)
                .isActive(true)
                .emailVerified(true)
                .build();

        when(userRepository.findByEmail("u@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login("u@example.com", "wrong"));
        assertEquals("Invalid credentials", ex.getMessage());
    }

    @Test
    void login_success_shouldIssueAndPersistRefreshToken() {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("u@example.com")
                .passwordHash("hash")
                .role(Role.HR)
                .companyId(companyId)
                .status(AccountStatus.ACTIVE)
                .isActive(true)
                .emailVerified(true)
                .build();

        when(userRepository.findByEmail("u@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "hash")).thenReturn(true);
        when(jwtService.generateAccessToken(userId.toString(), Role.HR, companyId.toString())).thenReturn("access");
        when(jwtService.generateRefreshToken()).thenReturn("refreshRaw");
        when(jwtService.hashToken("refreshRaw")).thenReturn("refreshHash");

        AuthTokens tokens = authService.login("u@example.com", "pass");

        assertEquals("access", tokens.getAccessToken());
        assertEquals("refreshRaw", tokens.getRefreshToken());
        assertEquals(userId, tokens.getUser().getId());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void refreshToken_shouldRejectInvalid() {
        when(jwtService.hashToken("bad")).thenReturn("badHash");
        when(refreshTokenRepository.findByTokenHash("badHash")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.refreshToken("bad"));
        assertEquals("Invalid refresh token", ex.getMessage());
    }

    @Test
    void refreshToken_shouldRotateAndRevokeOld() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("u@example.com")
                .passwordHash("hash")
                .role(Role.CANDIDATE)
                .status(AccountStatus.ACTIVE)
                .isActive(true)
                .emailVerified(true)
                .build();

        RefreshToken existing = RefreshToken.builder()
                .userId(userId)
                .tokenHash("oldHash")
                .expiresAt(ZonedDateTime.now().plusMinutes(5))
                .revoked(false)
                .build();

        when(jwtService.hashToken("oldRaw")).thenReturn("oldHash");
        when(refreshTokenRepository.findByTokenHash("oldHash")).thenReturn(Optional.of(existing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(userId.toString(), Role.CANDIDATE, null)).thenReturn("newAccess");
        when(jwtService.generateRefreshToken()).thenReturn("newRaw");
        when(jwtService.hashToken("newRaw")).thenReturn("newHash");

        AuthTokens res = authService.refreshToken("oldRaw");

        assertEquals("newAccess", res.getAccessToken());
        assertEquals("newRaw", res.getRefreshToken());
        assertTrue(existing.isRevoked());
        verify(refreshTokenRepository, atLeast(2)).save(any(RefreshToken.class));
    }

    @Test
    void refreshToken_shouldRejectWhenRevoked() {
        RefreshToken existing = RefreshToken.builder()
                .userId(UUID.randomUUID())
                .tokenHash("h")
                .expiresAt(ZonedDateTime.now().plusMinutes(5))
                .revoked(true)
                .build();

        when(jwtService.hashToken("raw")).thenReturn("h");
        when(refreshTokenRepository.findByTokenHash("h")).thenReturn(Optional.of(existing));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.refreshToken("raw"));
        assertEquals("Invalid or expired refresh token", ex.getMessage());
    }

    @Test
    void refreshToken_shouldRejectWhenExpired() {
        RefreshToken existing = RefreshToken.builder()
                .userId(UUID.randomUUID())
                .tokenHash("h")
                .expiresAt(ZonedDateTime.now().minusMinutes(1))
                .revoked(false)
                .build();

        when(jwtService.hashToken("raw")).thenReturn("h");
        when(refreshTokenRepository.findByTokenHash("h")).thenReturn(Optional.of(existing));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.refreshToken("raw"));
        assertEquals("Invalid or expired refresh token", ex.getMessage());
    }

    @Test
    void logout_shouldRevokeIfExists() {
        RefreshToken token = RefreshToken.builder()
                .userId(UUID.randomUUID())
                .tokenHash("h")
                .expiresAt(ZonedDateTime.now().plusMinutes(5))
                .revoked(false)
                .build();

        when(jwtService.hashToken("raw")).thenReturn("h");
        when(refreshTokenRepository.findByTokenHash("h")).thenReturn(Optional.of(token));

        authService.logout("raw");

        assertTrue(token.isRevoked());
        verify(refreshTokenRepository).save(token);
    }
}
