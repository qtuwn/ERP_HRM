package com.vthr.erp_hrm.infrastructure.persistence.mapper;

import com.vthr.erp_hrm.core.model.AuditLog;
import com.vthr.erp_hrm.infrastructure.persistence.entity.AuditLogEntity;

public class AuditLogMapper {

    public static AuditLog toDomain(AuditLogEntity entity) {
        if (entity == null) {
            return null;
        }
        return AuditLog.builder()
                .id(entity.getId())
                .actorId(entity.getActorId())
                .action(entity.getAction())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .payloadJson(entity.getPayloadJson())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static AuditLogEntity toEntity(AuditLog domain) {
        if (domain == null) {
            return null;
        }
        return AuditLogEntity.builder()
                .id(domain.getId())
                .actorId(domain.getActorId())
                .action(domain.getAction())
                .targetType(domain.getTargetType())
                .targetId(domain.getTargetId())
                .payloadJson(domain.getPayloadJson())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
