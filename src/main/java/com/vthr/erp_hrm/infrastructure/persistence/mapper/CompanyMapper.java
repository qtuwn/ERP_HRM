package com.vthr.erp_hrm.infrastructure.persistence.mapper;

import com.vthr.erp_hrm.core.model.Company;
import com.vthr.erp_hrm.infrastructure.persistence.entity.CompanyEntity;

public class CompanyMapper {

    public static Company toDomain(CompanyEntity entity) {
        if (entity == null) {
            return null;
        }
        return Company.builder()
                .id(entity.getId())
                .name(entity.getName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static CompanyEntity toEntity(Company domain) {
        if (domain == null) {
            return null;
        }
        return CompanyEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
