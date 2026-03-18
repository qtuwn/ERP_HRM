package com.vthr.erp_hrm.core.service;

import com.vthr.erp_hrm.core.model.AuthTokens;
import com.vthr.erp_hrm.core.model.User;

import java.util.UUID;

public interface AuthService {
    AuthTokens login(String email, String password);
    AuthTokens refreshToken(String refreshTokenStr);
    void logout(String refreshTokenStr);
    User register(String email, String password, String fullName, String phone);
    void changePassword(UUID userId, String oldPassword, String newPassword);
}
