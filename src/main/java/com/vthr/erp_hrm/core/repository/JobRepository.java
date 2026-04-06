package com.vthr.erp_hrm.core.repository;

import com.vthr.erp_hrm.core.model.Job;
import com.vthr.erp_hrm.core.model.JobStatus;
import com.vthr.erp_hrm.core.model.OpenJobsKeysetSlice;
import com.vthr.erp_hrm.core.model.PublicJobFilterOptions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
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

    /**
     * @param afterCreatedAt null cùng lúc với afterId = trang đầu
     */
    OpenJobsKeysetSlice findOpenJobsSearchKeyset(
            String q,
            String city,
            String industry,
            String jobType,
            String level,
            String skill,
            ZonedDateTime afterCreatedAt,
            UUID afterId,
            int limit);

    PublicJobFilterOptions findDistinctFilterOptionsForOpenJobs();

    /** Tên công ty (distinct) có ít nhất một tin OPEN — menu career / lọc theo công ty. */
    List<String> findDistinctCompanyNamesForOpenJobs();

    Page<Job> findByDepartment(String department, Pageable pageable);

    Page<Job> findByCompanyId(java.util.UUID companyId, Pageable pageable);

    Page<Job> findByCompanyIdAndCreatedBy(UUID companyId, UUID createdBy, Pageable pageable);

    java.util.List<Job> findByStatusAndExpiresAtBefore(String status, java.time.ZonedDateTime expiresAt);

    long count();

    long countByStatus(JobStatus status);

    /** Đếm job theo trạng thái (OPEN, CLOSED, …) phục vụ analytics. */
    Map<String, Long> countJobsGroupedByStatus();

    Job save(Job job);

    void deleteById(UUID id);
}
