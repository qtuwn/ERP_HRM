package com.vthr.erp_hrm.infrastructure.persistence.mapper;

import com.vthr.erp_hrm.core.model.Department;
import com.vthr.erp_hrm.infrastructure.persistence.entity.DepartmentEntity;

public class DepartmentMapper {

    public static Department toDomain(DepartmentEntity entity) {
        if (entity == null) return null;
        return Department.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .name(entity.getName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static DepartmentEntity toEntity(Department domain) {
        if (domain == null) return null;
        return DepartmentEntity.builder()
                .id(domain.getId())
                .companyId(domain.getCompanyId())
                .name(domain.getName())
                .build();
    }
}
