package com.vthr.erp_hrm.core.model;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
public class CompanyMember {
    private UUID id;
    private UUID companyId;
    private UUID userId;
    private String memberRole;
    private String status;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}
