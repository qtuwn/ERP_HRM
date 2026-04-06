package com.vthr.erp_hrm.core.model;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
public class Company {
    private UUID id;
    private String name;
    private boolean isVerifiedByAdmin;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}
