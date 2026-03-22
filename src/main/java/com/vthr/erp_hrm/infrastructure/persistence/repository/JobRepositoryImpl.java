package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.core.model.Job;
import com.vthr.erp_hrm.core.model.JobStatus;
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
