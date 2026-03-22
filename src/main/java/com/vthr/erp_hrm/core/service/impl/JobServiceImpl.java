package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.Job;
import com.vthr.erp_hrm.core.model.JobStatus;
import com.vthr.erp_hrm.core.repository.JobRepository;
import com.vthr.erp_hrm.core.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;

    @Override
    public Page<Job> getOpenJobs(Pageable pageable) {
        return jobRepository.findByStatus(JobStatus.OPEN, pageable);
    }

    @Override
    public Page<Job> getAllJobs(Pageable pageable) {
        return jobRepository.findAll(pageable);
    }

    @Override
    public Job getJobById(UUID id) {
        return jobRepository.findById(id).orElseThrow(() -> new RuntimeException("Job not found"));
    }

    @Override
    public Job getPublicJobById(UUID id) {
        Job job = getJobById(id);
        if (job.getStatus() != JobStatus.OPEN) {
            throw new RuntimeException("Job is not open");
        }
        return job;
    }

    @Override
    public Job createJob(Job job, UUID createdBy) {
        job.setStatus(JobStatus.DRAFT);
        job.setCreatedBy(createdBy);
        return jobRepository.save(job);
    }

    @Override
    public Job updateJob(UUID id, Job jobDetails) {
        Job existing = getJobById(id);
        
        existing.setTitle(jobDetails.getTitle());
        existing.setDescription(jobDetails.getDescription());
        existing.setDepartment(jobDetails.getDepartment());
        existing.setRequiredSkills(jobDetails.getRequiredSkills());
        existing.setExpiresAt(jobDetails.getExpiresAt());
        
        return jobRepository.save(existing);
    }

    @Override
    public Job publishJob(UUID id) {
        Job existing = getJobById(id);
        if (existing.getStatus() == JobStatus.OPEN) {
            throw new RuntimeException("Job is already open");
        }
        if (existing.getExpiresAt() == null || existing.getExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new RuntimeException("Cannot publish job with missing or past expiration date");
        }
        existing.setStatus(JobStatus.OPEN);
        return jobRepository.save(existing);
    }

    @Override
    public Job closeJob(UUID id) {
        Job existing = getJobById(id);
        if (existing.getStatus() == JobStatus.CLOSED) {
            throw new RuntimeException("Job is already closed");
        }
        existing.setStatus(JobStatus.CLOSED);
        return jobRepository.save(existing);
    }

    @Override
    public void deleteJob(UUID id) {
        jobRepository.deleteById(id);
    }
}
