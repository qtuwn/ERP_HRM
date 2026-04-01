package com.vthr.erp_hrm.core.repository;

import com.vthr.erp_hrm.core.model.SkillCategory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkillCategoryRepository {
    List<SkillCategory> findAll();

    Optional<SkillCategory> findById(UUID id);

    Optional<SkillCategory> findByNameIgnoreCase(String name);

    SkillCategory save(SkillCategory category);

    void deleteById(UUID id);
}

