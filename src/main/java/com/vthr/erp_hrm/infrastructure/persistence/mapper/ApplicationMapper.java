package com.vthr.erp_hrm.infrastructure.persistence.mapper;

import com.vthr.erp_hrm.core.model.Application;
import com.vthr.erp_hrm.core.model.ApplicationStatus;
import com.vthr.erp_hrm.infrastructure.persistence.entity.ApplicationEntity;

public class ApplicationMapper {

    public static Application toDomain(ApplicationEntity entity) {
        if (entity == null) return null;
        return Application.builder()
                .id(entity.getId())
                .jobId(entity.getJobId())
                .candidateId(entity.getCandidateId())
                .cvUrl(entity.getCvUrl())
                .status(entity.getStatus() != null ? ApplicationStatus.valueOf(entity.getStatus()) : null)
                .aiStatus(entity.getAiStatus())
                .formData(entity.getFormData())
                .cvText(entity.getCvText())
                .hrNote(entity.getHrNote())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static ApplicationEntity toEntity(Application domain) {
        if (domain == null) return null;
        ApplicationEntity entity = new ApplicationEntity();
        entity.setId(domain.getId());
        entity.setJobId(domain.getJobId());
        entity.setCandidateId(domain.getCandidateId());
        entity.setCvUrl(domain.getCvUrl());
        entity.setStatus(domain.getStatus() != null ? domain.getStatus().name() : ApplicationStatus.APPLIED.name());
        entity.setAiStatus(domain.getAiStatus() != null ? domain.getAiStatus() : "PENDING");
        entity.setFormData(domain.getFormData());
        entity.setCvText(domain.getCvText());
        entity.setHrNote(domain.getHrNote());
        return entity;
    }
}
