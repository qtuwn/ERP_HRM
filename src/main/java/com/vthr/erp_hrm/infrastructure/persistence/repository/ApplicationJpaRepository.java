package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.infrastructure.persistence.entity.ApplicationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationJpaRepository extends JpaRepository<ApplicationEntity, UUID> {
    Page<ApplicationEntity> findByJobId(UUID jobId, Pageable pageable);
    List<ApplicationEntity> findByJobId(UUID jobId);
    Page<ApplicationEntity> findByCandidateId(UUID candidateId, Pageable pageable);
    boolean existsByJobIdAndCandidateId(UUID jobId, UUID candidateId);
}
