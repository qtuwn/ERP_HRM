package com.vthr.erp_hrm.infrastructure.controller.response;

import com.vthr.erp_hrm.core.model.Department;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
public class DepartmentResponse {
    private UUID id;
    private UUID companyId;
    private String name;
    private ZonedDateTime createdAt;

    public static DepartmentResponse fromDomain(Department dept) {
        if (dept == null) return null;
        return DepartmentResponse.builder()
                .id(dept.getId())
                .companyId(dept.getCompanyId())
                .name(dept.getName())
                .createdAt(dept.getCreatedAt())
                .build();
    }
}
