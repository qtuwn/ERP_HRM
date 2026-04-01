package com.vthr.erp_hrm.core.repository;

import com.vthr.erp_hrm.core.model.Skill;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkillRepository {
    List<Skill> findAll();

    List<Skill> findByCategoryId(UUID categoryId);

    Optional<Skill> findById(UUID id);

    Optional<Skill> findByNameIgnoreCase(String name);

    Skill save(Skill skill);

    void deleteById(UUID id);
}

