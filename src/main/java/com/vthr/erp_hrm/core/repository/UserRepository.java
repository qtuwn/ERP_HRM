package com.vthr.erp_hrm.core.repository;

import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    List<User> findByRole(Role role);
    User save(User user);
    void deleteById(UUID id);
}
