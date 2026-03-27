package com.vthr.erp_hrm.infrastructure.persistence.mapper;

import com.vthr.erp_hrm.core.model.CompanyMember;
import com.vthr.erp_hrm.infrastructure.persistence.entity.CompanyMemberEntity;

public class CompanyMemberMapper {

    public static CompanyMember toDomain(CompanyMemberEntity entity) {
        if (entity == null) {
            return null;
        }
        return CompanyMember.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .userId(entity.getUserId())
                .memberRole(entity.getMemberRole())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static CompanyMemberEntity toEntity(CompanyMember domain) {
        if (domain == null) {
            return null;
        }
        return CompanyMemberEntity.builder()
                .id(domain.getId())
                .companyId(domain.getCompanyId())
                .userId(domain.getUserId())
                .memberRole(domain.getMemberRole())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
