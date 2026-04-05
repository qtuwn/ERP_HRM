package com.vthr.erp_hrm.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "applications", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"job_id", "candidate_id"})
})
@Getter
@Setter
public class ApplicationEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "cv_url", nullable = false, length = 500)
    private String cvUrl;

    @Column(nullable = false)
    private String status;

    @Column(name = "ai_status", length = 50)
    private String aiStatus;

    @Column(name = "form_data", columnDefinition = "TEXT")
    private String formData;

    @Column(name = "cv_text", columnDefinition = "TEXT")
    private String cvText;

    @Column(name = "hr_note", columnDefinition = "TEXT")
    private String hrNote;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
}
