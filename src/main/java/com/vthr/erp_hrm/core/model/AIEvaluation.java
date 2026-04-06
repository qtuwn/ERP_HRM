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
public class AIEvaluation {
    private UUID id;
    private UUID applicationId;
    private Integer score;
    private String matchedSkills;
    private String missingSkills;
    private String summary;
    private String discrepancy;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}
