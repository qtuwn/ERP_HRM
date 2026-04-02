package com.vthr.erp_hrm.core.repository;

import com.vthr.erp_hrm.core.model.Job;
import com.vthr.erp_hrm.core.model.JobStatus;
import com.vthr.erp_hrm.core.model.PublicJobFilterOptions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface JobRepository {
    Optional<Job> findById(UUID id);

    Page<Job> findAll(Pageable pageable);

    Page<Job> findByStatus(JobStatus status, Pageable pageable);

    /**
     * Việc làm public OPEN. Tham số rỗng = bỏ lọc theo trường đó.
     */
    Page<Job> findOpenJobsSearch(
            String q,
            String city,
            String industry,
            String jobType,
            String level,
            String skill,
            Pageable pageable);

    PublicJobFilterOptions findDistinctFilterOptionsForOpenJobs();

    Page<Job> findByDepartment(String department, Pageable pageable);

    Page<Job> findByCompanyId(java.util.UUID companyId, Pageable pageable);

    Page<Job> findByCompanyIdAndCreatedBy(UUID companyId, UUID createdBy, Pageable pageable);

    java.util.List<Job> findByStatusAndExpiresAtBefore(String status, java.time.ZonedDateTime expiresAt);

    long count();

    long countByStatus(JobStatus status);

    Job save(Job job);

    void deleteById(UUID id);
}
