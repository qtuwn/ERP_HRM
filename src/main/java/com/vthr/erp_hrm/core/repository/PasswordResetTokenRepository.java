package com.vthr.erp_hrm.core.repository;

import com.vthr.erp_hrm.core.model.PasswordResetToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    List<PasswordResetToken> findActiveByUserId(UUID userId);

    PasswordResetToken save(PasswordResetToken token);
}
