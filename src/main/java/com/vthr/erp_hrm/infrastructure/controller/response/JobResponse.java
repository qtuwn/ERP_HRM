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
    private String title;
    private String description;
    private String department;
    private String requiredSkills;
    private String status;
    private ZonedDateTime expiresAt;
    private UUID createdBy;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    public static JobResponse fromDomain(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .department(job.getDepartment())
                .requiredSkills(job.getRequiredSkills())
                .status(job.getStatus() != null ? job.getStatus().name() : null)
                .expiresAt(job.getExpiresAt())
                .createdBy(job.getCreatedBy())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}
