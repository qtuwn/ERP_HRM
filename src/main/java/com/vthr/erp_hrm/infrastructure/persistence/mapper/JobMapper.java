package com.vthr.erp_hrm.infrastructure.persistence.mapper;

import com.vthr.erp_hrm.core.model.Job;
import com.vthr.erp_hrm.core.model.JobStatus;
import com.vthr.erp_hrm.infrastructure.persistence.entity.JobEntity;

public class JobMapper {

    public static Job toDomain(JobEntity entity) {
        if (entity == null)
            return null;
        return Job.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .industry(entity.getIndustry())
                .level(entity.getLevel())
                .jobType(entity.getJobType())
                .salaryType(entity.getSalaryType())
                .salaryMin(entity.getSalaryMin())
                .salaryMax(entity.getSalaryMax())
                .salaryCurrency(entity.getSalaryCurrency())
                .description(entity.getDescription())
                .requirements(entity.getRequirements())
                .benefits(entity.getBenefits())
                .requiredSkills(entity.getRequiredSkills())
                .tags(entity.getTags())
                .companyName(entity.getCompanyName())
                .companyLogo(entity.getCompanyLogo())
                .address(entity.getAddress())
                .city(entity.getCity())
                .companySize(entity.getCompanySize())
                .department(entity.getDepartment())
                .notificationEmail(entity.getNotificationEmail())
                .numberOfPositions(entity.getNumberOfPositions())
                .status(JobStatus.valueOf(entity.getStatus()))
                .approvalStatus(entity.getApprovalStatus())
                .expiresAt(entity.getExpiresAt())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static JobEntity toEntity(Job domain) {
        if (domain == null)
            return null;
        JobEntity entity = new JobEntity();
        entity.setId(domain.getId());
        entity.setTitle(domain.getTitle());
        entity.setIndustry(domain.getIndustry());
        entity.setLevel(domain.getLevel());
        entity.setJobType(domain.getJobType());
        entity.setSalaryType(domain.getSalaryType());
        entity.setSalaryMin(domain.getSalaryMin());
        entity.setSalaryMax(domain.getSalaryMax());
        entity.setSalaryCurrency(domain.getSalaryCurrency());
        entity.setDescription(domain.getDescription());
        entity.setRequirements(domain.getRequirements());
        entity.setBenefits(domain.getBenefits());
        entity.setRequiredSkills(domain.getRequiredSkills());
        entity.setTags(domain.getTags());
        entity.setCompanyName(domain.getCompanyName());
        entity.setCompanyLogo(domain.getCompanyLogo());
        entity.setAddress(domain.getAddress());
        entity.setCity(domain.getCity());
        entity.setCompanySize(domain.getCompanySize());
        entity.setDepartment(domain.getDepartment());
        entity.setNotificationEmail(domain.getNotificationEmail());
        entity.setNumberOfPositions(domain.getNumberOfPositions());
        entity.setStatus(domain.getStatus() != null ? domain.getStatus().name() : JobStatus.DRAFT.name());
        entity.setApprovalStatus(domain.getApprovalStatus());
        entity.setExpiresAt(domain.getExpiresAt());
        entity.setCreatedBy(domain.getCreatedBy());
        return entity;
    }
}
