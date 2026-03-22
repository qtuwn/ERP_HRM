package com.vthr.erp_hrm.infrastructure.persistence.mapper;

import com.vthr.erp_hrm.core.model.Job;
import com.vthr.erp_hrm.core.model.JobStatus;
import com.vthr.erp_hrm.infrastructure.persistence.entity.JobEntity;

public class JobMapper {

    public static Job toDomain(JobEntity entity) {
        if (entity == null) return null;
        return Job.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .department(entity.getDepartment())
                .requiredSkills(entity.getRequiredSkills())
                .status(JobStatus.valueOf(entity.getStatus()))
                .expiresAt(entity.getExpiresAt())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static JobEntity toEntity(Job domain) {
        if (domain == null) return null;
        JobEntity entity = new JobEntity();
        entity.setId(domain.getId());
        entity.setTitle(domain.getTitle());
        entity.setDescription(domain.getDescription());
        entity.setDepartment(domain.getDepartment());
        entity.setRequiredSkills(domain.getRequiredSkills());
        entity.setStatus(domain.getStatus() != null ? domain.getStatus().name() : JobStatus.DRAFT.name());
        entity.setExpiresAt(domain.getExpiresAt());
        entity.setCreatedBy(domain.getCreatedBy());
        return entity;
    }
}
