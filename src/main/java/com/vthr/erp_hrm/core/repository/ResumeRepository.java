package com.vthr.erp_hrm.core.repository;

import com.vthr.erp_hrm.core.model.Resume;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResumeRepository {
    List<Resume> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Resume> findById(UUID id);
    long countByUserId(UUID userId);
    void unsetDefaultForUser(UUID userId);
    Resume save(Resume resume);
    void deleteById(UUID id);
}

