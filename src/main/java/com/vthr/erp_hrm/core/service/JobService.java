package com.vthr.erp_hrm.core.service;

import com.vthr.erp_hrm.core.model.Job;
import com.vthr.erp_hrm.core.model.OpenJobsKeysetSlice;
import com.vthr.erp_hrm.core.model.PublicJobFilterOptions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public interface JobService {
    Page<Job> getOpenJobs(String search, Pageable pageable);

    Page<Job> getOpenJobs(
            String search,
            String city,
            String industry,
            String jobType,
            String level,
            String skill,
            Pageable pageable);

    /** Feed công khai phân trang keyset (cuộn / tải thêm), cùng bộ lọc với {@link #getOpenJobs}. */
    OpenJobsKeysetSlice getOpenJobsKeyset(
            String q,
            String city,
            String industry,
            String jobType,
            String level,
            String skill,
            ZonedDateTime afterCreatedAt,
            UUID afterId,
            int size);

    PublicJobFilterOptions getOpenJobFilterOptions();

    /** Tên công ty có tin tuyển OPEN (menu Công ty trên career site). */
    List<String> getCareerCompanyNames();

    Page<Job> getAllJobs(Pageable pageable);

    Page<Job> getJobsByDepartment(String department, Pageable pageable);

    Page<Job> getJobsByCompanyId(UUID companyId, Pageable pageable);

    /** Job thuộc công ty và do user đó tạo (HR). */
    Page<Job> getJobsByCompanyIdAndCreatedBy(UUID companyId, UUID createdByUserId, Pageable pageable);

    Job getJobById(UUID id);

    Job getPublicJobById(UUID id);

    Job createJob(Job job, UUID createdBy);

    Job updateJob(UUID id, Job jobDetails);

    Job publishJob(UUID id);

    Job closeJob(UUID id);

    void deleteJob(UUID id);
}
