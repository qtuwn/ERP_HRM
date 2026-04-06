package com.vthr.erp_hrm.core.service;

/**
 * Giới hạn lạm dụng quên mật khẩu: theo IP (request/confirm) và khóa tạm theo email khi nhập OTP sai nhiều lần.
 */
public interface PasswordResetBruteForceProtection {

    void assertForgotPasswordRequestAllowed(String clientIp);

    void assertForgotPasswordConfirmAllowed(String clientIp);

    void assertEmailNotLockedForPasswordReset(String normalizedEmail);

    void recordPasswordResetConfirmFailure(String normalizedEmail);

    void clearPasswordResetConfirmFailures(String normalizedEmail);
}
