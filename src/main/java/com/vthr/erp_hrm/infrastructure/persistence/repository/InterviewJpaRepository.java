package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.infrastructure.persistence.entity.InterviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InterviewJpaRepository extends JpaRepository<InterviewEntity, UUID> {
    List<InterviewEntity> findByApplicationId(UUID applicationId);
}
