package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.core.model.Application;
import com.vthr.erp_hrm.core.repository.ApplicationRepository;
import com.vthr.erp_hrm.infrastructure.persistence.entity.ApplicationEntity;
import com.vthr.erp_hrm.infrastructure.persistence.mapper.ApplicationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ApplicationRepositoryImpl implements ApplicationRepository {

    private final ApplicationJpaRepository jpaRepository;

    @Override
    public Optional<Application> findById(UUID id) {
        return jpaRepository.findById(id).map(ApplicationMapper::toDomain);
    }

    @Override
    public Page<Application> findByJobId(UUID jobId, Pageable pageable) {
        return jpaRepository.findByJobId(jobId, pageable).map(ApplicationMapper::toDomain);
    }

    @Override
    public List<Application> findByJobId(UUID jobId) {
        return jpaRepository.findByJobId(jobId).stream()
                .map(ApplicationMapper::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public Page<Application> findByCandidateId(UUID candidateId, Pageable pageable) {
        return jpaRepository.findByCandidateId(candidateId, pageable).map(ApplicationMapper::toDomain);
    }

    @Override
    public boolean existsByJobIdAndCandidateId(UUID jobId, UUID candidateId) {
        return jpaRepository.existsByJobIdAndCandidateId(jobId, candidateId);
    }

    @Override
    public Application save(Application application) {
        ApplicationEntity entity = ApplicationMapper.toEntity(application);
        return ApplicationMapper.toDomain(jpaRepository.save(entity));
    }
}
