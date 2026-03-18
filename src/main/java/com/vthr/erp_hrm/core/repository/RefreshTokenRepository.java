package com.vthr.erp_hrm.core.repository;

import com.vthr.erp_hrm.core.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    RefreshToken save(RefreshToken refreshToken);
    void revokeAllUserTokens(UUID userId);
}
