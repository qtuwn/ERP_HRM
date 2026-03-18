package com.vthr.erp_hrm.infrastructure.persistence.mapper;

import com.vthr.erp_hrm.core.model.AIEvaluation;
import com.vthr.erp_hrm.infrastructure.persistence.entity.AIEvaluationEntity;

public class AIEvaluationMapper {
    public static AIEvaluation toDomain(AIEvaluationEntity entity) {
        if (entity == null) return null;
        return AIEvaluation.builder()
                .id(entity.getId())
                .applicationId(entity.getApplicationId())
                .score(entity.getScore())
                .matchedSkills(entity.getMatchedSkills())
                .missingSkills(entity.getMissingSkills())
                .summary(entity.getSummary())
                .discrepancy(entity.getDiscrepancy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static AIEvaluationEntity toEntity(AIEvaluation domain) {
        if (domain == null) return null;
        AIEvaluationEntity entity = new AIEvaluationEntity();
        entity.setId(domain.getId());
        entity.setApplicationId(domain.getApplicationId());
        entity.setScore(domain.getScore());
        entity.setMatchedSkills(domain.getMatchedSkills());
        entity.setMissingSkills(domain.getMissingSkills());
        entity.setSummary(domain.getSummary());
        entity.setDiscrepancy(domain.getDiscrepancy());
        return entity;
    }
}
