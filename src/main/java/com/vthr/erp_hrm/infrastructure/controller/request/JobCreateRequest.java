package com.vthr.erp_hrm.infrastructure.controller.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.vthr.erp_hrm.infrastructure.config.jackson.LenientZonedDateTimeDeserializer;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class JobCreateRequest {
    @NotBlank
    private String title;

    // Basic Info
    private String industry;
    private String level;
    private String jobType;

    // Salary Info
    private String salaryType; // range, agreed, upto
    private Long salaryMin;
    private Long salaryMax;
    private String salaryCurrency;

    // Job Descriptions
    @NotBlank
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
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm[:ss][.SSS][XXX]")
    @JsonDeserialize(using = LenientZonedDateTimeDeserializer.class)
    private ZonedDateTime expiresAt;
}
