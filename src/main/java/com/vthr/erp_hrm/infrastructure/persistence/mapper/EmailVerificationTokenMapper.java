package com.vthr.erp_hrm.infrastructure.persistence.mapper;

import com.vthr.erp_hrm.core.model.EmailVerificationToken;
import com.vthr.erp_hrm.infrastructure.persistence.entity.EmailVerificationTokenEntity;
import org.springframework.stereotype.Component;

@Component
public class EmailVerificationTokenMapper {

    public EmailVerificationToken toDomain(EmailVerificationTokenEntity entity) {
        if (entity == null) {
            return null;
        }
        return EmailVerificationToken.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .tokenHash(entity.getTokenHash())
                .expiresAt(entity.getExpiresAt())
                .usedAt(entity.getUsedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public EmailVerificationTokenEntity toEntity(EmailVerificationToken domain) {
        if (domain == null) {
            return null;
        }
        return EmailVerificationTokenEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .tokenHash(domain.getTokenHash())
                .expiresAt(domain.getExpiresAt())
                .usedAt(domain.getUsedAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
