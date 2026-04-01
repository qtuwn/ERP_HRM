package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.core.model.SkillCategory;
import com.vthr.erp_hrm.core.repository.SkillCategoryRepository;
import com.vthr.erp_hrm.infrastructure.persistence.mapper.SkillCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SkillCategoryRepositoryImpl implements SkillCategoryRepository {
    private final SkillCategoryJpaRepository jpa;

    @Override
    public List<SkillCategory> findAll() {
        return jpa.findAll().stream().map(SkillCategoryMapper::toDomain).toList();
    }

    @Override
    public Optional<SkillCategory> findById(UUID id) {
        return jpa.findById(id).map(SkillCategoryMapper::toDomain);
    }

    @Override
    public Optional<SkillCategory> findByNameIgnoreCase(String name) {
        return jpa.findByNameIgnoreCase(name).map(SkillCategoryMapper::toDomain);
    }

    @Override
    public SkillCategory save(SkillCategory category) {
        var saved = jpa.save(SkillCategoryMapper.toEntity(category));
        return SkillCategoryMapper.toDomain(saved);
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }
}

