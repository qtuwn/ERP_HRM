package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.infrastructure.persistence.entity.ApplicationTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationTaskJpaRepository extends JpaRepository<ApplicationTaskEntity, UUID> {

    List<ApplicationTaskEntity> findByApplicationIdOrderByCreatedAtDesc(UUID applicationId);

    @Query("select distinct t from ApplicationTaskEntity t left join fetch t.attachments where t.id = :id")
    Optional<ApplicationTaskEntity> findDetailById(@Param("id") UUID id);

    boolean existsByIdAndApplicationId(UUID id, UUID applicationId);
}
