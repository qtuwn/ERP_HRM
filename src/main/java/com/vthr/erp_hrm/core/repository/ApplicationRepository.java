package com.vthr.erp_hrm.core.repository;

import com.vthr.erp_hrm.core.model.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository {
    Optional<Application> findById(UUID id);
    Page<Application> findByJobId(UUID jobId, Pageable pageable);
    List<Application> findByJobId(UUID jobId);
    long count();
    long countByStatus(String status);

    /** Một query GROUP BY thay cho nhiều lần countByStatus (dashboard). */
    Map<String, Long> countApplicationsGroupedByStatus();

    Page<Application> findByCandidateId(UUID candidateId, Pageable pageable);
    boolean existsByJobIdAndCandidateId(UUID jobId, UUID candidateId);

    Optional<Application> findByJobIdAndCandidateId(UUID jobId, UUID candidateId);

    void deleteById(UUID id);

    Application save(Application application);
}
