package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.infrastructure.persistence.entity.SkillEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkillJpaRepository extends JpaRepository<SkillEntity, UUID> {
    List<SkillEntity> findByCategoryIdOrderByNameAsc(UUID categoryId);

    Optional<SkillEntity> findByNameIgnoreCase(String name);

    List<SkillEntity> findAllByOrderByNameAsc();
}

