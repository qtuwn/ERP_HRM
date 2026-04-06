package com.vthr.erp_hrm.core.repository;

import com.vthr.erp_hrm.core.model.AIEvaluation;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AIEvaluationRepository {
    Optional<AIEvaluation> findByApplicationId(UUID applicationId);

    List<AIEvaluation> findAllByApplicationIdIn(Collection<UUID> applicationIds);

    AIEvaluation save(AIEvaluation evaluation);
}
