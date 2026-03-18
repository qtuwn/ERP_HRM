package com.vthr.erp_hrm.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {
    private UUID id;
    private String title;
    private String description;
    private String department;
    private String requiredSkills;
    private JobStatus status;
    private ZonedDateTime expiresAt;
    private UUID createdBy;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}
