package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.core.model.RefreshToken;
import com.vthr.erp_hrm.core.repository.RefreshTokenRepository;
import com.vthr.erp_hrm.infrastructure.persistence.entity.RefreshTokenEntity;
import com.vthr.erp_hrm.infrastructure.persistence.mapper.RefreshTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;
    private final RefreshTokenMapper mapper;

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenEntity entity = mapper.toEntity(refreshToken);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public void revokeAllUserTokens(UUID userId) {
        jpaRepository.revokeAllTokensByUserId(userId);
    }
}
