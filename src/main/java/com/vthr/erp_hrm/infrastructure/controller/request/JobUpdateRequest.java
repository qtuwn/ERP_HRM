package com.vthr.erp_hrm.infrastructure.controller.request;

import lombok.Data;
import java.time.ZonedDateTime;

@Data
public class JobUpdateRequest {
    private String title;

    // Basic Info
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
    private String department;
    private String notificationEmail;
    private Integer numberOfPositions;

    // Other
    private ZonedDateTime expiresAt;
}
