package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.core.model.EmailVerificationToken;
import com.vthr.erp_hrm.core.repository.EmailVerificationTokenRepository;
import com.vthr.erp_hrm.infrastructure.persistence.mapper.EmailVerificationTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EmailVerificationTokenRepositoryImpl implements EmailVerificationTokenRepository {

    private final EmailVerificationTokenJpaRepository jpaRepository;
    private final EmailVerificationTokenMapper mapper;

    @Override
    public Optional<EmailVerificationToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public List<EmailVerificationToken> findActiveByUserId(UUID userId) {
        return jpaRepository.findByUserIdAndUsedAtIsNull(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public EmailVerificationToken save(EmailVerificationToken token) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(token)));
    }
}
