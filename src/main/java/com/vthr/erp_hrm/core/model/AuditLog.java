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
public class AuditLog {
    private UUID id;
    private UUID actorId;
    private String action;
    private String targetType;
    private UUID targetId;
    private String payloadJson;
    private ZonedDateTime createdAt;
}
