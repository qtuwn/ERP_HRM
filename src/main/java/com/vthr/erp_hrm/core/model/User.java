package com.vthr.erp_hrm.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private UUID id;
    private String email;
    private String passwordHash;
    private Role role;
    private AccountStatus status;
    private boolean isActive;
    private boolean mustChangePassword;
    private String fullName;
    private UUID companyId;
    private String department;
    private String phone;
    private boolean emailVerified;
    private ZonedDateTime verifiedAt;
    private ZonedDateTime deletedAt;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}
