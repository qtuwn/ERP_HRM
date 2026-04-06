package com.vthr.erp_hrm.core.service;

import com.vthr.erp_hrm.core.model.AuthTokens;
import com.vthr.erp_hrm.core.model.User;

import java.util.UUID;

public interface AuthService {
    AuthTokens login(String email, String password);

    AuthTokens refreshToken(String refreshTokenStr);

    void logout(String refreshTokenStr);

    User register(String email, String password, String fullName, String phone, String accountType, String companyName,
            String department);

    void verifyEmail(String token);

    void verifyEmailOtp(String email, String otp);

    void resendVerification(String email);

    void resendVerificationOtp(String email);

    void requestForgotPasswordOtp(String email);

    void requestForgotPasswordMagicLink(String email);

    void resetPasswordWithOtp(String email, String otp, String newPassword);

    void resetPasswordWithMagicLink(String token, String newPassword);

    void changePassword(UUID userId, String oldPassword, String newPassword);
}
