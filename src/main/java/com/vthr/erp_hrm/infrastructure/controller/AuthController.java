package com.vthr.erp_hrm.infrastructure.controller;

import com.vthr.erp_hrm.core.model.AuthTokens;
import com.vthr.erp_hrm.core.model.User;
import com.vthr.erp_hrm.core.service.AuthService;
import com.vthr.erp_hrm.infrastructure.controller.request.ChangePasswordRequest;
import com.vthr.erp_hrm.infrastructure.controller.request.LoginRequest;
import com.vthr.erp_hrm.infrastructure.controller.request.LogoutRequest;
import com.vthr.erp_hrm.infrastructure.controller.request.RefreshTokenRequest;
import com.vthr.erp_hrm.infrastructure.controller.request.RegisterRequest;
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

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request.getEmail(), request.getPassword(), request.getFullName(), request.getPhone());
        return ResponseEntity.ok(ApiResponse.success(UserResponse.fromDomain(user), "Registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthTokens tokens = authService.login(request.getEmail(), request.getPassword());
        LoginResponse data = LoginResponse.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .user(UserResponse.fromDomain(tokens.getUser()))
                .build();
        return ResponseEntity.ok(ApiResponse.success(data, "Login successful"));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthTokens tokens = authService.refreshToken(request.getRefreshToken());
        LoginResponse data = LoginResponse.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .user(UserResponse.fromDomain(tokens.getUser()))
                .build();
        return ResponseEntity.ok(ApiResponse.success(data, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Unauthorized");
        }
        UUID userId = UUID.fromString(authentication.getName());
        authService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        return ResponseEntity.ok(ApiResponse.success(null, "Email verified successfully (Demo)"));
    }
}
