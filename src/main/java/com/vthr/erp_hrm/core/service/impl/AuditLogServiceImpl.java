package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.AuditLog;
import com.vthr.erp_hrm.core.service.AuditLogService;
import com.vthr.erp_hrm.infrastructure.persistence.entity.AuditLogEntity;
import com.vthr.erp_hrm.infrastructure.persistence.mapper.AuditLogMapper;
import com.vthr.erp_hrm.infrastructure.persistence.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    public void logAction(UUID actorId, String action, String targetType, UUID targetId, String payloadJson) {
        AuditLogEntity logEntity = AuditLogEntity.builder()
                .id(UUID.randomUUID())
                .actorId(actorId)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .payloadJson(payloadJson)
                .build();
        auditLogRepository.save(logEntity);
    }

    @Override
    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc()
                .stream()
                .map(AuditLogMapper::toDomain)
                .collect(Collectors.toList());
    }
}
