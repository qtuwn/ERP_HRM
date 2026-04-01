package com.vthr.erp_hrm.infrastructure.controller.response;

import com.vthr.erp_hrm.core.model.AccountStatus;
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
    private AccountStatus status;
    private boolean isActive;
    private String fullName;
    private UUID companyId;
    private String companyName;
    private UUID departmentId;
    private String department;
    private String phone;
    private boolean emailVerified;
    private ZonedDateTime createdAt;

    public static UserResponse fromDomain(User user) {
        return fromDomain(user, null);
    }

    public static UserResponse fromDomain(User user, String companyName) {
        if (user == null)
            return null;
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .isActive(user.isActive())
                .fullName(user.getFullName())
                .companyId(user.getCompanyId())
                .companyName(companyName)
                .departmentId(user.getDepartmentId())
                .department(user.getDepartment())
                .phone(user.getPhone())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
