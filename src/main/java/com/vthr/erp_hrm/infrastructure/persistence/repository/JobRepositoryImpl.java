package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.core.model.Job;
import com.vthr.erp_hrm.core.model.JobStatus;
import com.vthr.erp_hrm.core.model.PublicJobFilterOptions;
import com.vthr.erp_hrm.core.repository.JobRepository;
import com.vthr.erp_hrm.infrastructure.persistence.entity.JobEntity;
import com.vthr.erp_hrm.infrastructure.persistence.mapper.JobMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JobRepositoryImpl implements JobRepository {

    private final JobJpaRepository jobJpaRepository;

    @Override
    public Optional<Job> findById(UUID id) {
        return jobJpaRepository.findById(id).map(JobMapper::toDomain);
    }

    @Override
    public Page<Job> findAll(Pageable pageable) {
        return jobJpaRepository.findAll(pageable).map(JobMapper::toDomain);
    }

    @Override
    public Page<Job> findByStatus(JobStatus status, Pageable pageable) {
        return jobJpaRepository.findByStatus(status.name(), pageable).map(JobMapper::toDomain);
    }

    @Override
    public Page<Job> findOpenJobsSearch(
            String q,
            String city,
            String industry,
            String jobType,
            String level,
            String skill,
            Pageable pageable) {
        return jobJpaRepository.searchOpenJobs(
                JobStatus.OPEN.name(),
                nz(q),
                nz(city),
                nz(industry),
                nz(jobType),
                nz(level),
                nz(skill),
                pageable
        ).map(JobMapper::toDomain);
    }

    @Override
    public PublicJobFilterOptions findDistinctFilterOptionsForOpenJobs() {
        String st = JobStatus.OPEN.name();
        return new PublicJobFilterOptions(
                jobJpaRepository.findDistinctCitiesByStatus(st),
                jobJpaRepository.findDistinctIndustriesByStatus(st),
                jobJpaRepository.findDistinctJobTypesByStatus(st),
                jobJpaRepository.findDistinctLevelsByStatus(st)
        );
    }

    private static String nz(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        return s.trim();
    }

    @Override
    public Page<Job> findByDepartment(String department, Pageable pageable) {
        return jobJpaRepository.findByDepartment(department, pageable).map(JobMapper::toDomain);
    }

    @Override
    public Page<Job> findByCompanyId(UUID companyId, Pageable pageable) {
        return jobJpaRepository.findByCompanyId(companyId, pageable).map(JobMapper::toDomain);
    }

    @Override
    public Page<Job> findByCompanyIdAndCreatedBy(UUID companyId, UUID createdBy, Pageable pageable) {
        return jobJpaRepository.findByCompanyIdAndCreatedBy(companyId, createdBy, pageable).map(JobMapper::toDomain);
    }

    @Override
    public java.util.List<Job> findByStatusAndExpiresAtBefore(String status, java.time.ZonedDateTime expiresAt) {
        return jobJpaRepository.findByStatusAndExpiresAtBefore(status, expiresAt)
                .stream()
                .map(JobMapper::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public long count() {
        return jobJpaRepository.count();
    }

    @Override
    public long countByStatus(JobStatus status) {
        return jobJpaRepository.countByStatus(status.name());
    }

    @Override
    public Job save(Job job) {
        JobEntity entity = JobMapper.toEntity(job);
        JobEntity saved = jobJpaRepository.save(entity);
        return JobMapper.toDomain(saved);
    }

    @Override
    public void deleteById(UUID id) {
        jobJpaRepository.deleteById(id);
    }
}
