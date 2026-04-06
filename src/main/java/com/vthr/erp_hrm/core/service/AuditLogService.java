package com.vthr.erp_hrm.core.service;

import com.vthr.erp_hrm.core.model.AuditLog;

import java.util.List;
import java.util.UUID;

public interface AuditLogService {
    void logAction(UUID actorId, String action, String targetType, UUID targetId, String payloadJson);
    List<AuditLog> getRecentLogs();
}
