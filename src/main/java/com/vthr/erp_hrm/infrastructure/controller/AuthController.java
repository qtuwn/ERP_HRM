package com.vthr.erp_hrm.infrastructure.controller;

import com.vthr.erp_hrm.core.model.AuthTokens;
import com.vthr.erp_hrm.core.model.User;
import com.vthr.erp_hrm.core.service.AuthService;
import com.vthr.erp_hrm.infrastructure.persistence.repository.CompanyRepository;
import com.vthr.erp_hrm.infrastructure.controller.request.ChangePasswordRequest;
import com.vthr.erp_hrm.infrastructure.controller.request.ForgotPasswordRequest;
import com.vthr.erp_hrm.infrastructure.controller.request.LoginRequest;
import com.vthr.erp_hrm.infrastructure.controller.request.LogoutRequest;
import com.vthr.erp_hrm.infrastructure.controller.request.RefreshTokenRequest;
import com.vthr.erp_hrm.infrastructure.controller.request.RegisterRequest;
import com.vthr.erp_hrm.infrastructure.controller.request.ResetPasswordWithOtpRequest;
import com.vthr.erp_hrm.infrastructure.controller.request.ResendVerificationRequest;
import com.vthr.erp_hrm.infrastructure.controller.request.VerifyEmailOtpRequest;
import com.vthr.erp_hrm.infrastructure.controller.response.ApiResponse;
import com.vthr.erp_hrm.infrastructure.controller.response.LoginResponse;
import com.vthr.erp_hrm.infrastructure.controller.response.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CompanyRepository companyRepository;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(
                request.getEmail(),
                request.getPassword(),
                request.getFullName(),
                request.getPhone(),
                request.getAccountType(),
                request.getCompanyName(),
                request.getDepartment());
        return ResponseEntity.ok(ApiResponse.success(UserResponse.fromDomain(user), "Registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthTokens tokens = authService.login(request.getEmail(), request.getPassword());
        String companyName = resolveCompanyName(tokens.getUser());
        LoginResponse data = LoginResponse.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .user(UserResponse.fromDomain(tokens.getUser(), companyName))
                .build();
        return ResponseEntity.ok(ApiResponse.success(data, "Login successful"));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthTokens tokens = authService.refreshToken(request.getRefreshToken());
        String companyName = resolveCompanyName(tokens.getUser());
        LoginResponse data = LoginResponse.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .user(UserResponse.fromDomain(tokens.getUser(), companyName))
                .build();
        return ResponseEntity.ok(ApiResponse.success(data, "Token refreshed successfully"));
    }

    private String resolveCompanyName(User user) {
        if (user == null || user.getCompanyId() == null) return null;
        return companyRepository.findById(user.getCompanyId())
                .map(com.vthr.erp_hrm.infrastructure.persistence.entity.CompanyEntity::getName)
                .orElse(null);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Unauthorized");
        }
        UUID userId = UUID.fromString(authentication.getName());
        authService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success(null, "Email verified successfully"));
    }

    @PostMapping("/verify-email-otp")
    public ResponseEntity<ApiResponse<Void>> verifyEmailOtp(@Valid @RequestBody VerifyEmailOtpRequest request) {
        authService.verifyEmailOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.success(null, "Email verified successfully"));
    }

    // Alias for Facebook-like flow naming
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody VerifyEmailOtpRequest request) {
        authService.verifyEmailOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.success(null, "OTP verified successfully"));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerification(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(null, "Verification email sent"));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerification(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(null, "OTP resent"));
    }

    @PostMapping("/forgot-password/request")
    public ResponseEntity<ApiResponse<Void>> requestForgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestForgotPasswordOtp(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(null, "OTP reset password da duoc gui"));
    }

    @PostMapping("/forgot-password/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmForgotPassword(
            @Valid @RequestBody ResetPasswordWithOtpRequest request) {
        authService.resetPasswordWithOtp(request.getEmail(), request.getOtp(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Reset password thanh cong"));
    }
}
