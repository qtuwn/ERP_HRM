package com.vthr.erp_hrm.core.service;

import com.vthr.erp_hrm.core.model.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface JobService {
    Page<Job> getOpenJobs(Pageable pageable);
    Page<Job> getAllJobs(Pageable pageable);
    Job getJobById(UUID id);
    Job getPublicJobById(UUID id);
    Job createJob(Job job, UUID createdBy);
    Job updateJob(UUID id, Job jobDetails);
    Job publishJob(UUID id);
    Job closeJob(UUID id);
    void deleteJob(UUID id);
}
