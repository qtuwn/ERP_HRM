package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.core.model.PasswordResetToken;
import com.vthr.erp_hrm.core.repository.PasswordResetTokenRepository;
import com.vthr.erp_hrm.infrastructure.persistence.mapper.PasswordResetTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PasswordResetTokenRepositoryImpl implements PasswordResetTokenRepository {

    private final PasswordResetTokenJpaRepository jpaRepository;
    private final PasswordResetTokenMapper mapper;

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public List<PasswordResetToken> findActiveByUserId(UUID userId) {
        return jpaRepository.findByUserIdAndUsedAtIsNull(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(token)));
    }
}
