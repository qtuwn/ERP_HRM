package com.vthr.erp_hrm.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@Getter
@Setter
public class JobEntity {

    @Id
    @GeneratedValue
    private UUID id;

    // Basic Info
    @Column(nullable = false)
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
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description; // Job description - with rich text

    @Column(columnDefinition = "TEXT")
    private String requirements; // Requirements for candidates - with rich text

    @Column(columnDefinition = "TEXT")
    private String benefits; // Benefits - with rich text

    private String requiredSkills; // Comma-separated tags
    private String tags; // Additional tags/skills

    // Company Info
    private String companyName;
    private String companyLogo; // URL or file path
    private String address; // Detailed address
    private String city; // Province/City for filtering
    private String companySize; // e.g., 50-100, 100-500, etc.

    // HR Logic
    private String department;
    private String notificationEmail; // Email to receive notification of applications
    private Integer numberOfPositions; // Number of positions to hire

    // Status & Metadata
    @Column(nullable = false)
    private String status; // DRAFT, OPEN, CLOSED

    private String approvalStatus; // PENDING, APPROVED, REJECTED

    @Column(name = "expires_at")
    private ZonedDateTime expiresAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
}
