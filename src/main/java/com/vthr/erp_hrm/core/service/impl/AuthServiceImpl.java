package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.AuthTokens;
import com.vthr.erp_hrm.core.model.AccountStatus;
import com.vthr.erp_hrm.core.model.EmailVerificationToken;
import com.vthr.erp_hrm.core.model.PasswordResetToken;
import com.vthr.erp_hrm.core.model.RefreshToken;
import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.model.User;
import com.vthr.erp_hrm.core.repository.EmailVerificationTokenRepository;
import com.vthr.erp_hrm.core.repository.PasswordResetTokenRepository;
import com.vthr.erp_hrm.core.repository.RefreshTokenRepository;
import com.vthr.erp_hrm.core.repository.UserRepository;
import com.vthr.erp_hrm.core.service.CompanyService;
import com.vthr.erp_hrm.core.model.Company;
import com.vthr.erp_hrm.infrastructure.email.EmailQueueService;
import com.vthr.erp_hrm.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vthr.erp_hrm.core.service.AuthService;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailQueueService emailQueueService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpirationMs;

    @Value("${auth.email-verification-expiration-hours:24}")
    private long emailVerificationExpirationHours;

    @Value("${auth.otp-expiration-minutes:5}")
    private long otpExpirationMinutes;

    @Value("${auth.otp-resend-cooldown-seconds:60}")
    private long otpResendCooldownSeconds;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Value("${auth.email-verification-demo-log-only:false}")
    private boolean emailVerificationDemoLogOnly;

    @Override
    public AuthTokens login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (user.getStatus() == AccountStatus.DELETED) {
            throw new RuntimeException("Invalid credentials");
        }

        if (user.getStatus() == AccountStatus.SUSPENDED) {
            throw new RuntimeException("User is suspended");
        }

        if (!user.isEmailVerified() || user.getStatus() == AccountStatus.PENDING) {
            throw new RuntimeException("Email is not verified");
        }

        if (!user.isActive()) {
            throw new RuntimeException("User is disabled");
        }

        String accessToken = jwtService.generateAccessToken(
                user.getId().toString(),
                user.getRole(),
                user.getCompanyId() != null ? user.getCompanyId().toString() : null
        );
        String refreshTokenStr = jwtService.generateRefreshToken();
        String tokenHash = jwtService.hashToken(refreshTokenStr);

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .expiresAt(ZonedDateTime.now().plus(refreshExpirationMs, ChronoUnit.MILLIS))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        return AuthTokens.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .user(user)
                .build();
    }

    @Override
    public AuthTokens refreshToken(String refreshTokenStr) {
        String tokenHash = jwtService.hashToken(refreshTokenStr);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.isRevoked() || refreshToken.getExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        String newAccessToken = jwtService.generateAccessToken(
                user.getId().toString(),
                user.getRole(),
                user.getCompanyId() != null ? user.getCompanyId().toString() : null
        );
        String newRefreshTokenStr = jwtService.generateRefreshToken();
        String newTokenHash = jwtService.hashToken(newRefreshTokenStr);

        RefreshToken newRefreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(newTokenHash)
                .expiresAt(ZonedDateTime.now().plus(refreshExpirationMs, ChronoUnit.MILLIS))
                .revoked(false)
                .build();

        refreshTokenRepository.save(newRefreshToken);

        return AuthTokens.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenStr)
                .user(user)
                .build();
    }

    @Override
    public void logout(String refreshTokenStr) {
        String tokenHash = jwtService.hashToken(refreshTokenStr);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    private final CompanyService companyService;

    @Override
    @Transactional
    public User register(String email, String password, String fullName, String phone, String accountType,
            String companyName, String department) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        Role role = resolveRole(accountType);

        UUID companyId = null;
        if (role == Role.HR || role == Role.COMPANY) {
            if (companyName == null || companyName.isBlank()) {
                throw new RuntimeException("Company name is required for HR registration");
            }
            Company company = companyService.createOrGetCompany(companyName.trim());
            companyId = company.getId();
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .isActive(true)
                .status(AccountStatus.PENDING)
                .mustChangePassword(false)
                .fullName(fullName)
                .companyId(companyId)
                .department(blankToNull(department))
                .phone(phone)
                .emailVerified(false)
                .verifiedAt(null)
                .deletedAt(null)
                .build();
        User saved = userRepository.save(user);

        if (companyId != null) {
            companyService.addHrMember(companyId, saved.getId(), role.name());
        }

        // UI hiện tại dùng OTP verify email (/verify-otp). Tạo OTP thật thay vì chỉ magic-link.
        createEmailVerificationOtpAndDispatch(saved);
        return saved;
    }

    @Override
    public void verifyEmail(String token) {
        String tokenHash = jwtService.hashToken(token);
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

        if (verificationToken.getUsedAt() != null) {
            throw new RuntimeException("Verification token already used");
        }

        if (verificationToken.getExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new RuntimeException("Verification token expired");
        }

        User user = userRepository.findById(verificationToken.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getStatus() == AccountStatus.DELETED) {
            throw new RuntimeException("User no longer available");
        }

        user.setEmailVerified(true);
        user.setVerifiedAt(ZonedDateTime.now());
        user.setStatus(AccountStatus.ACTIVE);
        user.setActive(true);
        userRepository.save(user);

        verificationToken.setUsedAt(ZonedDateTime.now());
        emailVerificationTokenRepository.save(verificationToken);
    }

    @Override
    public void resendVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getStatus() == AccountStatus.DELETED) {
            throw new RuntimeException("User no longer available");
        }

        if (user.isEmailVerified() && user.getStatus() == AccountStatus.ACTIVE) {
            return;
        }

        enforceOtpResendCooldown(user.getId());
        createEmailVerificationAndDispatch(user);
    }

    @Override
    public void resendVerificationOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getStatus() == AccountStatus.DELETED) {
            throw new RuntimeException("User no longer available");
        }

        if (user.isEmailVerified() && user.getStatus() == AccountStatus.ACTIVE) {
            return;
        }

        enforceOtpResendCooldown(user.getId());
        createEmailVerificationOtpAndDispatch(user);
    }

    @Override
    public void verifyEmailOtp(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String tokenHash = jwtService.hashToken(buildScopedOtpKey(user.getEmail(), otp));
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RuntimeException("OTP khong hop le"));

        if (!verificationToken.getUserId().equals(user.getId())) {
            throw new RuntimeException("OTP khong hop le");
        }

        if (verificationToken.getUsedAt() != null) {
            throw new RuntimeException("OTP da duoc su dung");
        }

        if (verificationToken.getExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new RuntimeException("OTP da het han");
        }

        user.setEmailVerified(true);
        user.setVerifiedAt(ZonedDateTime.now());
        user.setStatus(AccountStatus.ACTIVE);
        user.setActive(true);
        userRepository.save(user);

        // OTP one-time use: delete token after successful verification
        emailVerificationTokenRepository.deleteById(verificationToken.getId());
    }

    @Override
    public void requestForgotPasswordOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getStatus() == AccountStatus.DELETED || user.getStatus() == AccountStatus.SUSPENDED) {
            throw new RuntimeException("Tai khoan khong the reset mat khau");
        }

        ZonedDateTime now = ZonedDateTime.now();
        for (PasswordResetToken token : passwordResetTokenRepository.findActiveByUserId(user.getId())) {
            token.setUsedAt(now);
            passwordResetTokenRepository.save(token);
        }

        String otp = generateOtp();
        String tokenHash = jwtService.hashToken(buildScopedOtpKey(user.getEmail(), otp));

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .expiresAt(now.plus(otpExpirationMinutes, ChronoUnit.MINUTES))
                .build();
        passwordResetTokenRepository.save(resetToken);

        emailQueueService.enqueueEmail(
                user.getEmail(),
                "OTP quen mat khau VTHR",
                "forgot_password_otp",
                Map.of(
                        "fullName", user.getFullName(),
                        "otp", otp,
                        "minutes", otpExpirationMinutes));
    }

    @Override
    public void resetPasswordWithOtp(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String tokenHash = jwtService.hashToken(buildScopedOtpKey(user.getEmail(), otp));
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RuntimeException("OTP khong hop le"));

        if (!resetToken.getUserId().equals(user.getId())) {
            throw new RuntimeException("OTP khong hop le");
        }

        if (resetToken.getUsedAt() != null) {
            throw new RuntimeException("OTP da duoc su dung");
        }

        if (resetToken.getExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new RuntimeException("OTP da het han");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsedAt(ZonedDateTime.now());
        passwordResetTokenRepository.save(resetToken);
    }

    @Override
    public void changePassword(java.util.UUID userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private void createEmailVerificationAndDispatch(User user) {
        ZonedDateTime now = ZonedDateTime.now();
        for (EmailVerificationToken token : emailVerificationTokenRepository.findActiveByUserId(user.getId())) {
            token.setUsedAt(now);
            emailVerificationTokenRepository.save(token);
        }

        // Magic-link token: demo nhanh hơn OTP vì chỉ cần click link.
        // Token raw chỉ được log/send; DB chỉ lưu hash.
        String rawToken = UUID.randomUUID().toString().replace("-", "");
        String tokenHash = jwtService.hashToken(rawToken);

        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .expiresAt(now.plus(emailVerificationExpirationHours, ChronoUnit.HOURS))
                .build();
        emailVerificationTokenRepository.save(verificationToken);

        String encoded = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        String verifyUrl = appBaseUrl + "/api/auth/verify-email?token=" + encoded;

        if (emailVerificationDemoLogOnly) {
            // Demo mode: không gửi email, chỉ log link để copy/paste
            System.out.println("[DEMO][EMAIL_VERIFY_LINK] " + verifyUrl);
            return;
        }

        emailQueueService.enqueueEmail(
                user.getEmail(),
                "Xac thuc email tai khoan VTHR",
                "verify_email_link",
                Map.of(
                        "fullName", user.getFullName(),
                        "verifyUrl", verifyUrl,
                        "hours", emailVerificationExpirationHours));
    }

    private void createEmailVerificationOtpAndDispatch(User user) {
        ZonedDateTime now = ZonedDateTime.now();
        for (EmailVerificationToken token : emailVerificationTokenRepository.findActiveByUserId(user.getId())) {
            token.setUsedAt(now);
            emailVerificationTokenRepository.save(token);
        }

        String otp = generateOtp();
        String tokenHash = jwtService.hashToken(buildScopedOtpKey(user.getEmail(), otp));

        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .expiresAt(now.plus(otpExpirationMinutes, ChronoUnit.MINUTES))
                .build();
        emailVerificationTokenRepository.save(verificationToken);

        if (emailVerificationDemoLogOnly) {
            System.out.println("[DEMO][EMAIL_VERIFY_OTP] " + user.getEmail() + " -> " + otp);
            return;
        }

        emailQueueService.enqueueEmail(
                user.getEmail(),
                "OTP xac thuc email tai khoan VTHR",
                "verify_email_otp",
                Map.of(
                        "fullName", user.getFullName(),
                        "otp", otp,
                        "minutes", otpExpirationMinutes));
    }

    private void enforceOtpResendCooldown(UUID userId) {
        ZonedDateTime now = ZonedDateTime.now();
        emailVerificationTokenRepository.findLatestByUserId(userId).ifPresent(latest -> {
            if (latest.getCreatedAt() == null) {
                return;
            }
            long seconds = Duration.between(latest.getCreatedAt(), now).getSeconds();
            if (seconds < otpResendCooldownSeconds) {
                long wait = otpResendCooldownSeconds - seconds;
                throw new RuntimeException("Vui long cho " + wait + " giay truoc khi gui lai OTP");
            }
        });
    }

    private String generateOtp() {
        int value = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(value);
    }

    private Role resolveRole(String accountType) {
        if (accountType == null || accountType.isBlank()) {
            return Role.CANDIDATE;
        }
        String normalized = accountType.trim().toUpperCase(Locale.ROOT);
        if ("HR".equals(normalized)) {
            return Role.HR;
        }
        if ("COMPANY".equals(normalized) || "HR_MANAGER".equals(normalized)) {
            return Role.COMPANY;
        }
        return Role.CANDIDATE;
    }

    private String buildScopedOtpKey(String email, String otp) {
        return email.trim().toLowerCase(Locale.ROOT) + ":" + otp.trim();
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
