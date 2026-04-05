package com.vthr.erp_hrm.core.repository;

import com.vthr.erp_hrm.core.model.EmailVerificationToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository {
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    List<EmailVerificationToken> findActiveByUserId(UUID userId);

    EmailVerificationToken save(EmailVerificationToken token);

    Optional<EmailVerificationToken> findLatestByUserId(UUID userId);

    void deleteById(UUID id);
}
