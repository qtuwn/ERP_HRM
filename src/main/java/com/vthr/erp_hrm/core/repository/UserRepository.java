package com.vthr.erp_hrm.core.repository;

import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    List<User> findAll();

    Page<User> findAll(Pageable pageable);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    List<User> findByRole(Role role);

    Page<User> findByRole(Role role, Pageable pageable);

    List<User> findByCompanyId(UUID companyId);

    Page<User> findByCompanyId(UUID companyId, Pageable pageable);

    Page<User> findByCompanyIdAndRole(UUID companyId, Role role, Pageable pageable);

    Page<User> findByCompanyIdAndRoleIn(UUID companyId, Collection<Role> roles, Pageable pageable);

    Page<User> findByCompanyIdAndDepartmentId(UUID companyId, UUID departmentId, Pageable pageable);

    long countByRole(Role role);

    User save(User user);

    void deleteById(UUID id);
}
