package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.Job;
import com.vthr.erp_hrm.core.model.JobStatus;
import com.vthr.erp_hrm.core.model.PublicJobFilterOptions;
import com.vthr.erp_hrm.core.repository.JobRepository;
import com.vthr.erp_hrm.core.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;

    @Override
    public Page<Job> getOpenJobs(String search, Pageable pageable) {
        return getOpenJobs(search, null, null, null, null, null, pageable);
    }

    @Override
    public Page<Job> getOpenJobs(
            String search,
            String city,
            String industry,
            String jobType,
            String level,
            String skill,
            Pageable pageable) {
        return jobRepository.findOpenJobsSearch(search, city, industry, jobType, level, skill, pageable);
    }

    @Override
    public PublicJobFilterOptions getOpenJobFilterOptions() {
        return jobRepository.findDistinctFilterOptionsForOpenJobs();
    }

    @Override
    public Page<Job> getAllJobs(Pageable pageable) {
        return jobRepository.findAll(pageable);
    }

    @Override
    public Page<Job> getJobsByDepartment(String department, Pageable pageable) {
        return jobRepository.findByDepartment(department, pageable);
    }

    @Override
    public Page<Job> getJobsByCompanyId(UUID companyId, Pageable pageable) {
        return jobRepository.findByCompanyId(companyId, pageable);
    }

    @Override
    public Page<Job> getJobsByCompanyIdAndCreatedBy(UUID companyId, UUID createdByUserId, Pageable pageable) {
        return jobRepository.findByCompanyIdAndCreatedBy(companyId, createdByUserId, pageable);
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
        try {
            job.setStatus(JobStatus.DRAFT);
            job.setCreatedBy(createdBy);
            return jobRepository.save(job);
        } catch (Exception e) {
            log.error("FULL ERROR DETAILS: createJob failed. createdBy={}, job={}", createdBy, job, e);
            throw e;
        }
    }

    @Override
    public Job updateJob(UUID id, Job jobDetails) {
        Job existing = getJobById(id);

        try {
            existing.setTitle(jobDetails.getTitle());
            existing.setIndustry(jobDetails.getIndustry());
            existing.setLevel(jobDetails.getLevel());
            existing.setJobType(jobDetails.getJobType());
            existing.setSalaryType(jobDetails.getSalaryType());
            existing.setSalaryMin(jobDetails.getSalaryMin());
            existing.setSalaryMax(jobDetails.getSalaryMax());
            existing.setSalaryCurrency(jobDetails.getSalaryCurrency());

            existing.setDescription(jobDetails.getDescription());
            existing.setRequirements(jobDetails.getRequirements());
            existing.setBenefits(jobDetails.getBenefits());
            existing.setRequiredSkills(jobDetails.getRequiredSkills());
            existing.setTags(jobDetails.getTags());

            existing.setCompanyName(jobDetails.getCompanyName());
            existing.setCompanyLogo(jobDetails.getCompanyLogo());
            existing.setAddress(jobDetails.getAddress());
            existing.setCity(jobDetails.getCity());
            existing.setCompanySize(jobDetails.getCompanySize());

            existing.setDepartment(jobDetails.getDepartment());
            existing.setNotificationEmail(jobDetails.getNotificationEmail());
            existing.setNumberOfPositions(jobDetails.getNumberOfPositions());

            existing.setExpiresAt(jobDetails.getExpiresAt());

            return jobRepository.save(existing);
        } catch (Exception e) {
            log.error("FULL ERROR DETAILS: updateJob failed. id={}, jobDetails={}", id, jobDetails, e);
            throw e;
        }
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
