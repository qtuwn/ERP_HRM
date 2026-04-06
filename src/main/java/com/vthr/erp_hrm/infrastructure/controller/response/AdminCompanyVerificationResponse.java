package com.vthr.erp_hrm.infrastructure.controller.response;

import com.vthr.erp_hrm.infrastructure.persistence.entity.CompanyEntity;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
public class AdminCompanyVerificationResponse {
    private UUID id;
    private String name;
    private boolean isVerifiedByAdmin;
    private ZonedDateTime createdAt;

    public static AdminCompanyVerificationResponse fromEntity(CompanyEntity e) {
        if (e == null) {
            return null;
        }
        return AdminCompanyVerificationResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .isVerifiedByAdmin(e.isVerifiedByAdmin())
                .createdAt(e.getCreatedAt())
                .build();
    }
}

