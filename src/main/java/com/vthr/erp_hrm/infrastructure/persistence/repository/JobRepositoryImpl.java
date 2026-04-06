package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.core.model.Job;
import com.vthr.erp_hrm.core.model.JobStatus;
import com.vthr.erp_hrm.core.model.OpenJobsKeysetSlice;
import com.vthr.erp_hrm.core.model.PublicJobFilterOptions;
import com.vthr.erp_hrm.core.repository.JobRepository;
import com.vthr.erp_hrm.infrastructure.persistence.entity.JobEntity;
import com.vthr.erp_hrm.infrastructure.persistence.mapper.JobMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public OpenJobsKeysetSlice findOpenJobsSearchKeyset(
            String q,
            String city,
            String industry,
            String jobType,
            String level,
            String skill,
            ZonedDateTime afterCreatedAt,
            UUID afterId,
            int limit) {
        int fetch = Math.min(Math.max(limit, 1), 100) + 1;
        Pageable pageable = PageRequest.of(0, fetch);
        boolean hasCursor = afterCreatedAt != null && afterId != null;
        Page<JobEntity> page = jobJpaRepository.searchOpenJobsKeyset(
                JobStatus.OPEN.name(),
                nz(q),
                nz(city),
                nz(industry),
                nz(jobType),
                nz(level),
                nz(skill),
                hasCursor,
                afterCreatedAt,
                afterId,
                pageable);
        List<JobEntity> raw = page.getContent();
        boolean hasNext = raw.size() > limit;
        List<JobEntity> slice = hasNext ? raw.subList(0, limit) : raw;
        ZonedDateTime nextT = null;
        UUID nextId = null;
        if (hasNext && !slice.isEmpty()) {
            JobEntity last = slice.get(slice.size() - 1);
            nextT = last.getCreatedAt();
            nextId = last.getId();
        }
        List<Job> jobs = slice.stream().map(JobMapper::toDomain).toList();
        return new OpenJobsKeysetSlice(jobs, hasNext, nextT, nextId);
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

    @Override
    public List<String> findDistinctCompanyNamesForOpenJobs() {
        return jobJpaRepository.findDistinctCompanyNamesByStatus(JobStatus.OPEN.name());
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
    public Map<String, Long> countJobsGroupedByStatus() {
        Map<String, Long> map = new HashMap<>();
        for (Object[] row : jobJpaRepository.countGroupedByStatus()) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            String status = String.valueOf(row[0]);
            long n = row[1] instanceof Number num ? num.longValue() : 0L;
            map.put(status, n);
        }
        return map;
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
