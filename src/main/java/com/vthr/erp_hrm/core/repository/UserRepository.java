package com.vthr.erp_hrm.core.repository;

import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    User save(User user);

    void deleteById(UUID id);
}
