package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.core.model.AIEvaluation;
import com.vthr.erp_hrm.core.repository.AIEvaluationRepository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import com.vthr.erp_hrm.infrastructure.persistence.entity.AIEvaluationEntity;
import com.vthr.erp_hrm.infrastructure.persistence.mapper.AIEvaluationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AIEvaluationRepositoryImpl implements AIEvaluationRepository {
    private final AIEvaluationJpaRepository jpaRepository;

    @Override
    public Optional<AIEvaluation> findByApplicationId(UUID applicationId) {
        return jpaRepository.findByApplicationId(applicationId).map(AIEvaluationMapper::toDomain);
    }

    @Override
    public List<AIEvaluation> findAllByApplicationIdIn(Collection<UUID> applicationIds) {
        if (applicationIds == null || applicationIds.isEmpty()) {
            return Collections.emptyList();
        }
        return jpaRepository.findByApplicationIdIn(applicationIds).stream()
                .map(AIEvaluationMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public AIEvaluation save(AIEvaluation evaluation) {
        AIEvaluationEntity entity = AIEvaluationMapper.toEntity(evaluation);
        return AIEvaluationMapper.toDomain(jpaRepository.save(entity));
    }
}
