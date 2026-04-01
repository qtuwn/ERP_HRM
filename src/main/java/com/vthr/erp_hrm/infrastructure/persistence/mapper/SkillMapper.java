package com.vthr.erp_hrm.infrastructure.persistence.mapper;

import com.vthr.erp_hrm.core.model.Skill;
import com.vthr.erp_hrm.infrastructure.persistence.entity.SkillEntity;

public class SkillMapper {
    public static Skill toDomain(SkillEntity e) {
        if (e == null) {
            return null;
        }
        return Skill.builder()
                .id(e.getId())
                .categoryId(e.getCategoryId())
                .name(e.getName())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    public static SkillEntity toEntity(Skill d) {
        if (d == null) {
            return null;
        }
        return SkillEntity.builder()
                .id(d.getId())
                .categoryId(d.getCategoryId())
                .name(d.getName())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}

