package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.infrastructure.persistence.entity.AIEvaluationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AIEvaluationJpaRepository extends JpaRepository<AIEvaluationEntity, UUID> {
    Optional<AIEvaluationEntity> findByApplicationId(UUID applicationId);
}
