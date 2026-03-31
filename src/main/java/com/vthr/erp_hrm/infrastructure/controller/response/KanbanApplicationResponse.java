package com.vthr.erp_hrm.infrastructure.controller.response;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
public class KanbanApplicationResponse {
    private UUID id;
    private UUID candidateId;
    private String candidateName;
    private String candidateEmail;
    private String status;
    private String aiStatus;
    private Integer aiScore;
    private String aiSuitability;
    private String cvUrl;
    private ZonedDateTime createdAt;
}
