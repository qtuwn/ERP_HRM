package com.vthr.erp_hrm.infrastructure.persistence.mapper;

import com.vthr.erp_hrm.core.model.Resume;
import com.vthr.erp_hrm.infrastructure.persistence.entity.ResumeEntity;
import org.springframework.stereotype.Component;

@Component
public class ResumeMapper {
    public Resume toDomain(ResumeEntity entity) {
        if (entity == null) {
            return null;
        }
        return Resume.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .title(entity.getTitle())
                .storagePath(entity.getStoragePath())
                .originalFilename(entity.getOriginalFilename())
                .mimeType(entity.getMimeType())
                .sizeBytes(entity.getSizeBytes())
                .isDefault(entity.isDefault())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public ResumeEntity toEntity(Resume domain) {
        if (domain == null) {
            return null;
        }
        return ResumeEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .title(domain.getTitle())
                .storagePath(domain.getStoragePath())
                .originalFilename(domain.getOriginalFilename())
                .mimeType(domain.getMimeType())
                .sizeBytes(domain.getSizeBytes())
                .isDefault(domain.isDefault())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}

