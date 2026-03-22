package com.vthr.erp_hrm.infrastructure.controller.response;

import com.vthr.erp_hrm.core.model.Application;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
public class ApplicationResponse {
    private UUID id;
    private UUID jobId;
    private String jobTitle;
    private UUID candidateId;
    private String cvUrl;
    private String status;
    private String aiStatus;
    private String formData;
    private String cvText;
    private String hrNote;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    public static ApplicationResponse fromDomain(Application application) {
        return ApplicationResponse.builder()
                .id(application.getId())
                .jobId(application.getJobId())
                .candidateId(application.getCandidateId())
                .cvUrl(application.getCvUrl())
                .status(application.getStatus() != null ? application.getStatus().name() : null)
                .aiStatus(application.getAiStatus())
                .formData(application.getFormData())
                .cvText(application.getCvText())
                .hrNote(application.getHrNote())
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .build();
    }
}
