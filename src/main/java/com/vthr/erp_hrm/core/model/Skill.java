package com.vthr.erp_hrm.core.model;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
public class Skill {
    private UUID id;
    private UUID categoryId;
    private String name;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}

