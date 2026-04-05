package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.infrastructure.persistence.entity.JobEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobJpaRepository extends JpaRepository<JobEntity, UUID> {
    Page<JobEntity> findByStatus(String status, Pageable pageable);

    @Query("""
            SELECT j FROM JobEntity j
            WHERE j.status = :status
            AND (
              LOWER(j.title) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(COALESCE(j.companyName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(COALESCE(j.city, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(COALESCE(j.department, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            """)
    Page<JobEntity> findByStatusAndKeyword(@Param("status") String status, @Param("q") String q, Pageable pageable);

    Page<JobEntity> findByDepartment(String department, Pageable pageable);

    Page<JobEntity> findByCompanyId(UUID companyId, Pageable pageable);

    java.util.List<JobEntity> findByStatusAndExpiresAtBefore(String status, java.time.ZonedDateTime expiresAt);

    long countByStatus(String status);
}
