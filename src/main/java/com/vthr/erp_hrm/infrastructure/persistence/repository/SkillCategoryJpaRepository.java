package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.infrastructure.persistence.entity.SkillCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkillCategoryJpaRepository extends JpaRepository<SkillCategoryEntity, UUID> {
    Optional<SkillCategoryEntity> findByNameIgnoreCase(String name);
}

