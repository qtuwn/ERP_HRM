package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.infrastructure.persistence.entity.DepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentJpaRepository extends JpaRepository<DepartmentEntity, UUID> {
    List<DepartmentEntity> findByCompanyId(UUID companyId);

    Optional<DepartmentEntity> findByCompanyIdAndNameIgnoreCase(UUID companyId, String name);
}
