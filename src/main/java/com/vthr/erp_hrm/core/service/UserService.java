package com.vthr.erp_hrm.core.service;

import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.model.User;

import java.util.List;
import java.util.UUID;

public interface UserService {
    User getUserById(UUID id);
    User getUserByEmail(String email);
    List<User> getUsersByRole(Role role);
    User createUser(User user);
    User updateUser(User user);
    void deleteUser(UUID id);
}
