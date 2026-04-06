package com.vthr.erp_hrm.infrastructure.controller.response;

import com.vthr.erp_hrm.core.model.SkillCategory;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
public class SkillCategoryResponse {
    private UUID id;
    private String name;
    private ZonedDateTime createdAt;

    public static SkillCategoryResponse fromDomain(SkillCategory d) {
        if (d == null) {
            return null;
        }
        return SkillCategoryResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .createdAt(d.getCreatedAt())
                .build();
    }
}

