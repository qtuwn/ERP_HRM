package com.vthr.erp_hrm.infrastructure.controller.response;

import com.vthr.erp_hrm.core.model.Skill;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
public class SkillResponse {
    private UUID id;
    private UUID categoryId;
    private String name;
    private ZonedDateTime createdAt;

    public static SkillResponse fromDomain(Skill d) {
        if (d == null) {
            return null;
        }
        return SkillResponse.builder()
                .id(d.getId())
                .categoryId(d.getCategoryId())
                .name(d.getName())
                .createdAt(d.getCreatedAt())
                .build();
    }
}

