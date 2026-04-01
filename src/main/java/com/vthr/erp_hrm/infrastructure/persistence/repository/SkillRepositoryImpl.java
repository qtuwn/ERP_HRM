package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.core.model.Skill;
import com.vthr.erp_hrm.core.repository.SkillRepository;
import com.vthr.erp_hrm.infrastructure.persistence.mapper.SkillMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SkillRepositoryImpl implements SkillRepository {
    private final SkillJpaRepository jpa;

    @Override
    public List<Skill> findAll() {
        return jpa.findAllByOrderByNameAsc().stream().map(SkillMapper::toDomain).toList();
    }

    @Override
    public List<Skill> findByCategoryId(UUID categoryId) {
        return jpa.findByCategoryIdOrderByNameAsc(categoryId).stream().map(SkillMapper::toDomain).toList();
    }

    @Override
    public Optional<Skill> findById(UUID id) {
        return jpa.findById(id).map(SkillMapper::toDomain);
    }

    @Override
    public Optional<Skill> findByNameIgnoreCase(String name) {
        return jpa.findByNameIgnoreCase(name).map(SkillMapper::toDomain);
    }

    @Override
    public Skill save(Skill skill) {
        var saved = jpa.save(SkillMapper.toEntity(skill));
        return SkillMapper.toDomain(saved);
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }
}

