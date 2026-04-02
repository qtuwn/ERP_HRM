package com.vthr.erp_hrm.core.service;

import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UserService {
    List<User> getAllUsers();

    Page<User> getAllUsers(Pageable pageable);

    User getUserById(UUID id);

    User getUserByEmail(String email);

    List<User> getUsersByRole(Role role);

    Page<User> getUsersByRole(Role role, Pageable pageable);

    Page<User> getUsersByCompanyId(UUID companyId, Pageable pageable);

    Page<User> getUsersByCompanyIdAndRole(UUID companyId, Role role, Pageable pageable);

    Page<User> getUsersByCompanyIdAndRolesIn(UUID companyId, Collection<Role> roles, Pageable pageable);

    Page<User> getUsersByCompanyIdAndDepartmentId(UUID companyId, UUID departmentId, Pageable pageable);

    long countUsersByRole(Role role);

    User updateUserRole(UUID userId, Role role);

    User setUserActive(UUID userId, boolean active);

    User createUser(User user);

    User updateUser(User user);

    User updateProfile(UUID userId, String fullName, String phone);

    User updateDepartment(UUID userId, String department);

    void deleteUser(UUID id);
}
