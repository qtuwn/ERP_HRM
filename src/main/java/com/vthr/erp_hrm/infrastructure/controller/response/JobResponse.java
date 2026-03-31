package com.vthr.erp_hrm.infrastructure.controller.response;

import com.vthr.erp_hrm.core.model.Job;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
public class JobResponse {
    private UUID id;

    // Basic Info
    private String title;
    private String industry;
    private String level;
    private String jobType;

    // Salary Info
    private String salaryType;
    private Long salaryMin;
    private Long salaryMax;
    private String salaryCurrency;

    // Job Descriptions
    private String description;
    private String requirements;
    private String benefits;
    private String requiredSkills;
    private String tags;

    // Company Info
    private String companyName;
    private String companyLogo;
    private String address;
    private String city;
    private String companySize;

    // HR Logic
    private UUID companyId;
    private String department;
    private String notificationEmail;
    private Integer numberOfPositions;

    // Status
    private String status;
    private String approvalStatus;

    // Metadata
    private ZonedDateTime expiresAt;
    private UUID createdBy;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    public static JobResponse fromDomain(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .industry(job.getIndustry())
                .level(job.getLevel())
                .jobType(job.getJobType())
                .salaryType(job.getSalaryType())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .salaryCurrency(job.getSalaryCurrency())
                .description(job.getDescription())
                .requirements(job.getRequirements())
                .benefits(job.getBenefits())
                .requiredSkills(job.getRequiredSkills())
                .tags(job.getTags())
                .companyName(job.getCompanyName())
                .companyLogo(job.getCompanyLogo())
                .address(job.getAddress())
                .city(job.getCity())
                .companySize(job.getCompanySize())
                .companyId(job.getCompanyId())
                .department(job.getDepartment())
                .notificationEmail(job.getNotificationEmail())
                .numberOfPositions(job.getNumberOfPositions())
                .status(job.getStatus() != null ? job.getStatus().name() : null)
                .approvalStatus(job.getApprovalStatus())
                .expiresAt(job.getExpiresAt())
                .createdBy(job.getCreatedBy())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}
