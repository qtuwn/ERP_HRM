package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.infrastructure.persistence.entity.CompanyMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyMemberRepository extends JpaRepository<CompanyMemberEntity, UUID> {
    List<CompanyMemberEntity> findByCompanyId(UUID companyId);
    Optional<CompanyMemberEntity> findByUserId(UUID userId);
}
