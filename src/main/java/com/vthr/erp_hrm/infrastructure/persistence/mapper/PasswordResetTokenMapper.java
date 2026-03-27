package com.vthr.erp_hrm.infrastructure.persistence.mapper;

import com.vthr.erp_hrm.core.model.PasswordResetToken;
import com.vthr.erp_hrm.infrastructure.persistence.entity.PasswordResetTokenEntity;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetTokenMapper {

    public PasswordResetToken toDomain(PasswordResetTokenEntity entity) {
        if (entity == null) {
            return null;
        }
        return PasswordResetToken.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .tokenHash(entity.getTokenHash())
                .expiresAt(entity.getExpiresAt())
                .usedAt(entity.getUsedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public PasswordResetTokenEntity toEntity(PasswordResetToken domain) {
        if (domain == null) {
            return null;
        }
        return PasswordResetTokenEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .tokenHash(domain.getTokenHash())
                .expiresAt(domain.getExpiresAt())
                .usedAt(domain.getUsedAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
