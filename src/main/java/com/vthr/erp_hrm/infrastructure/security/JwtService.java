package com.vthr.erp_hrm.infrastructure.security;

import com.vthr.erp_hrm.core.model.Role;

public interface JwtService {
    String generateAccessToken(String subject, Role role);
    String generateRefreshToken();
    String hashToken(String token);
}
