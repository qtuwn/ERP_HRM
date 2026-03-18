package com.vthr.erp_hrm.infrastructure.controller.response;

import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.model.User;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String email;
    private Role role;
    private boolean isActive;
    private String fullName;
    private String department;
    private String phone;
    private boolean emailVerified;
    private ZonedDateTime createdAt;
    
    public static UserResponse fromDomain(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .isActive(user.isActive())
                .fullName(user.getFullName())
                .department(user.getDepartment())
                .phone(user.getPhone())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
