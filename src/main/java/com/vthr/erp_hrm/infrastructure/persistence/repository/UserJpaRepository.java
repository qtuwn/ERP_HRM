package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.core.model.AccountStatus;
import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmailAndStatusNot(String email, AccountStatus status);

    List<UserEntity> findByRoleAndStatusNot(Role role, AccountStatus status);

    Page<UserEntity> findByRoleAndStatusNot(Role role, AccountStatus status, Pageable pageable);

    long countByRoleAndStatusNot(Role role, AccountStatus status);

    List<UserEntity> findByStatusNot(AccountStatus status);

    Page<UserEntity> findByStatusNot(AccountStatus status, Pageable pageable);

    List<UserEntity> findByCompanyIdAndStatusNot(UUID companyId, AccountStatus status);

    Page<UserEntity> findByCompanyIdAndStatusNot(UUID companyId, AccountStatus status, Pageable pageable);

    Page<UserEntity> findByCompanyIdAndRoleAndStatusNot(UUID companyId, Role role, AccountStatus status, Pageable pageable);

    Page<UserEntity> findByCompanyIdAndDepartmentIdAndStatusNot(UUID companyId, UUID departmentId, AccountStatus status, Pageable pageable);
}
