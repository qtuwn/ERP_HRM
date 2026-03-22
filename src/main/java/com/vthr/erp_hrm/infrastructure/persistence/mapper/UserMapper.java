package com.vthr.erp_hrm.infrastructure.persistence.mapper;

import com.vthr.erp_hrm.core.model.User;
import com.vthr.erp_hrm.infrastructure.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public User toDomain(UserEntity entity) {
        if (entity == null) return null;
        return User.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .passwordHash(entity.getPasswordHash())
                .role(entity.getRole())
                .isActive(entity.isActive())
                .mustChangePassword(entity.isMustChangePassword())
                .fullName(entity.getFullName())
                .department(entity.getDepartment())
                .phone(entity.getPhone())
                .emailVerified(entity.isEmailVerified())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public UserEntity toEntity(User domain) {
        if (domain == null) return null;
        return UserEntity.builder()
                .id(domain.getId())
                .email(domain.getEmail())
                .passwordHash(domain.getPasswordHash())
                .role(domain.getRole())
                .isActive(domain.isActive())
                .mustChangePassword(domain.isMustChangePassword())
                .fullName(domain.getFullName())
                .department(domain.getDepartment())
                .phone(domain.getPhone())
                .emailVerified(domain.isEmailVerified())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
