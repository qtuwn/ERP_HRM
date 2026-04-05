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
public class Interview {
    private UUID id;
    private UUID applicationId;
    private ZonedDateTime interviewTime;
    private String locationOrLink;
    private UUID interviewerId;
    private String status;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}
