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

    // Basic Info
    private String title;
    private String industry;
    private String level; // Intern, Fresher, Junior, Senior, Manager
    private String jobType; // Full-time, Part-time, Internship, Freelance

    // Salary Info
    private String salaryType; // range, agreed, upto
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
    private String department;
    private String notificationEmail;
    private Integer numberOfPositions;

    // Status
    private JobStatus status;
    private String approvalStatus;

    // Metadata
    private ZonedDateTime expiresAt;
    private UUID createdBy;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}
