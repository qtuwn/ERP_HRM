package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.infrastructure.persistence.entity.ResumeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResumeJpaRepository extends JpaRepository<ResumeEntity, UUID> {
    List<ResumeEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    long countByUserId(UUID userId);

    @Modifying
    @Query("UPDATE ResumeEntity r SET r.isDefault = false WHERE r.userId = :userId AND r.isDefault = true")
    int unsetDefaultForUser(@Param("userId") UUID userId);
}

