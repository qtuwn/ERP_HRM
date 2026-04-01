package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.core.model.Resume;
import com.vthr.erp_hrm.core.repository.ResumeRepository;
import com.vthr.erp_hrm.infrastructure.persistence.mapper.ResumeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ResumeRepositoryImpl implements ResumeRepository {

    private final ResumeJpaRepository jpaRepository;
    private final ResumeMapper mapper;

    @Override
    public List<Resume> findByUserIdOrderByCreatedAtDesc(UUID userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Resume> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public long countByUserId(UUID userId) {
        return jpaRepository.countByUserId(userId);
    }

    @Override
    @Transactional
    public void unsetDefaultForUser(UUID userId) {
        jpaRepository.unsetDefaultForUser(userId);
    }

    @Override
    public Resume save(Resume resume) {
        if (resume.getId() == null) {
            resume.setId(UUID.randomUUID());
        }
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(resume)));
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}

