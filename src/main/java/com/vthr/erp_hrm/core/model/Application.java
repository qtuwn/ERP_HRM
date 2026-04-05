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
public class Application {
    private UUID id;
    private UUID jobId;
    private UUID candidateId;
    private String cvUrl;
    private ApplicationStatus status;
    private String aiStatus;
    private String formData;
    private String cvText;
    private String hrNote;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}
