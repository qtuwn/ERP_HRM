package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.infrastructure.persistence.entity.ApplicationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationJpaRepository extends JpaRepository<ApplicationEntity, UUID> {
    Optional<ApplicationEntity> findByJobIdAndCandidateId(UUID jobId, UUID candidateId);
    Page<ApplicationEntity> findByJobId(UUID jobId, Pageable pageable);
    List<ApplicationEntity> findByJobId(UUID jobId);
    long countByStatus(String status);

    @Query("SELECT a.status, COUNT(a) FROM ApplicationEntity a GROUP BY a.status")
    List<Object[]> countGroupedByStatus();

    Page<ApplicationEntity> findByCandidateId(UUID candidateId, Pageable pageable);
    boolean existsByJobIdAndCandidateId(UUID jobId, UUID candidateId);
}
