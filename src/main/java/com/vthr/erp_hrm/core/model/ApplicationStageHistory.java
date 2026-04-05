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
public class ApplicationStageHistory {
    private UUID id;
    private UUID applicationId;
    private ApplicationStatus fromStage;
    private ApplicationStatus toStage;
    private UUID changedBy;
    private String note;
    private ZonedDateTime createdAt;
}
