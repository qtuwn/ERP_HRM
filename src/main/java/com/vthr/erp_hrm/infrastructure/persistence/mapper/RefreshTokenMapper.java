package com.vthr.erp_hrm.infrastructure.persistence.mapper;

import com.vthr.erp_hrm.core.model.RefreshToken;
import com.vthr.erp_hrm.infrastructure.persistence.entity.RefreshTokenEntity;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenMapper {
    public RefreshToken toDomain(RefreshTokenEntity entity) {
        if (entity == null) return null;
        return RefreshToken.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .tokenHash(entity.getTokenHash())
                .expiresAt(entity.getExpiresAt())
                .revoked(entity.isRevoked())
                .createdAt(entity.getCreatedAt())
                .clientIp(entity.getClientIp())
                .userAgent(entity.getUserAgent())
                .build();
    }

    public RefreshTokenEntity toEntity(RefreshToken domain) {
        if (domain == null) return null;
        return RefreshTokenEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .tokenHash(domain.getTokenHash())
                .expiresAt(domain.getExpiresAt())
                .revoked(domain.isRevoked())
                .createdAt(domain.getCreatedAt())
                .clientIp(domain.getClientIp())
                .userAgent(domain.getUserAgent())
                .build();
    }
}
