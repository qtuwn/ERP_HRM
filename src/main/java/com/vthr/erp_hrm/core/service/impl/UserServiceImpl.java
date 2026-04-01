package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.AccountStatus;
import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.model.User;
import com.vthr.erp_hrm.core.repository.UserRepository;
import com.vthr.erp_hrm.core.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.ZonedDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    @Override
    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

    @Override
    public Page<User> getUsersByRole(Role role, Pageable pageable) {
        return userRepository.findByRole(role, pageable);
    }

    @Override
    public Page<User> getUsersByCompanyId(UUID companyId, Pageable pageable) {
        return userRepository.findByCompanyId(companyId, pageable);
    }

    @Override
    public Page<User> getUsersByCompanyIdAndRole(UUID companyId, Role role, Pageable pageable) {
        return userRepository.findByCompanyIdAndRole(companyId, role, pageable);
    }

    @Override
    public Page<User> getUsersByCompanyIdAndDepartmentId(UUID companyId, UUID departmentId, Pageable pageable) {
        return userRepository.findByCompanyIdAndDepartmentId(companyId, departmentId, pageable);
    }

    @Override
    public long countUsersByRole(Role role) {
        return userRepository.countByRole(role);
    }

    @Override
    public User updateUserRole(UUID userId, Role role) {
        User existing = getUserById(userId);
        existing.setRole(role);
        return userRepository.save(existing);
    }

    @Override
    public User setUserActive(UUID userId, boolean active) {
        User existing = getUserById(userId);
        existing.setActive(active);
        if (active) {
            existing.setStatus(existing.isEmailVerified() ? AccountStatus.ACTIVE : AccountStatus.PENDING);
        } else {
            existing.setStatus(AccountStatus.SUSPENDED);
        }
        return userRepository.save(existing);
    }

    @Override
    public User createUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public User updateProfile(UUID userId, String fullName, String phone) {
        User existing = getUserById(userId);
        if (fullName != null && !fullName.isBlank()) {
            existing.setFullName(fullName.trim());
        }
        if (phone != null && !phone.isBlank()) {
            existing.setPhone(phone.trim());
        }
        existing.setUpdatedAt(ZonedDateTime.now());
        return userRepository.save(existing);
    }

    @Override
    public User updateDepartment(UUID userId, String department) {
        User existing = getUserById(userId);
        existing.setDepartment(department != null ? department.trim() : null);
        existing.setUpdatedAt(ZonedDateTime.now());
        return userRepository.save(existing);
    }

    @Override
    public void deleteUser(UUID id) {
        User existing = getUserById(id);
        existing.setStatus(AccountStatus.DELETED);
        existing.setActive(false);
        existing.setDeletedAt(ZonedDateTime.now());
        userRepository.save(existing);
    }
}
