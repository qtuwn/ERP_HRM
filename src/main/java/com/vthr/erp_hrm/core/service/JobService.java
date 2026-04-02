package com.vthr.erp_hrm.core.service;

import com.vthr.erp_hrm.core.model.Job;
import com.vthr.erp_hrm.core.model.PublicJobFilterOptions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    PublicJobFilterOptions getOpenJobFilterOptions();

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
