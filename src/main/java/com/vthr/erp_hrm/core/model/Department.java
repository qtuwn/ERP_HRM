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
public class Department {
    private UUID id;
    private UUID companyId;
    private String name;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}
