package com.vthr.erp_hrm.core.repository;

import com.vthr.erp_hrm.core.model.RefreshToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    RefreshToken save(RefreshToken refreshToken);

    void revokeAllUserTokens(UUID userId);

    List<RefreshToken> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /** Thu hồi một refresh token; trả số bản ghi cập nhật (0 nếu không thuộc user hoặc đã revoke). */
    int revokeByIdForUser(UUID tokenId, UUID userId);
}
