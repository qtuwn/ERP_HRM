package com.vthr.erp_hrm.infrastructure.persistence.mapper;

import com.vthr.erp_hrm.core.model.SkillCategory;
import com.vthr.erp_hrm.infrastructure.persistence.entity.SkillCategoryEntity;

public class SkillCategoryMapper {
    public static SkillCategory toDomain(SkillCategoryEntity e) {
        if (e == null) {
            return null;
        }
        return SkillCategory.builder()
                .id(e.getId())
                .name(e.getName())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    public static SkillCategoryEntity toEntity(SkillCategory d) {
        if (d == null) {
            return null;
        }
        return SkillCategoryEntity.builder()
                .id(d.getId())
                .name(d.getName())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}

