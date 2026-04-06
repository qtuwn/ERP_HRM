package com.vthr.erp_hrm.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "interviews")
@Getter
@Setter
public class InterviewEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "interview_time", nullable = false)
    private ZonedDateTime interviewTime;

    @Column(name = "location_or_link", nullable = false)
    private String locationOrLink;

    @Column(name = "interviewer_id")
    private UUID interviewerId;

    @Column(nullable = false)
    private String status = "SCHEDULED";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
}
