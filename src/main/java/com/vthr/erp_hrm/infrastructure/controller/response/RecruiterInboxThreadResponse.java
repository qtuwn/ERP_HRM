package com.vthr.erp_hrm.infrastructure.controller.response;

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
public class RecruiterInboxThreadResponse {
    private UUID applicationId;
    private UUID jobId;
    private String jobTitle;
    private String candidateName;
    private String candidateEmail;
    private String status;
    private String lastMessagePreview;
    private ZonedDateTime lastMessageAt;
}
