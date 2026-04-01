package com.vthr.erp_hrm.core.repository;

import com.vthr.erp_hrm.core.model.Job;
import com.vthr.erp_hrm.core.model.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface JobRepository {
    Optional<Job> findById(UUID id);

    Page<Job> findAll(Pageable pageable);

    Page<Job> findByStatus(JobStatus status, Pageable pageable);

    /** Từ khóa null hoặc rỗng → chỉ lọc theo status OPEN (qua findByStatus). */
    Page<Job> findOpenJobsWithOptionalKeyword(String keyword, Pageable pageable);

    Page<Job> findByDepartment(String department, Pageable pageable);

    Page<Job> findByCompanyId(java.util.UUID companyId, Pageable pageable);

    java.util.List<Job> findByStatusAndExpiresAtBefore(String status, java.time.ZonedDateTime expiresAt);

    long count();

    long countByStatus(JobStatus status);

    Job save(Job job);

    void deleteById(UUID id);
}
