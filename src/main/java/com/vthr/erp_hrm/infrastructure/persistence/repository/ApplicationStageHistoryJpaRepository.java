package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.infrastructure.persistence.entity.ApplicationStageHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApplicationStageHistoryJpaRepository extends JpaRepository<ApplicationStageHistoryEntity, UUID> {
    List<ApplicationStageHistoryEntity> findByApplicationIdOrderByCreatedAtDesc(UUID applicationId);

    List<ApplicationStageHistoryEntity> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId);
}
