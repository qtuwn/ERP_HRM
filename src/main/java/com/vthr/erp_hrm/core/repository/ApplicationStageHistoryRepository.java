package com.vthr.erp_hrm.core.repository;

import com.vthr.erp_hrm.core.model.ApplicationStageHistory;
import java.util.List;
import java.util.UUID;

public interface ApplicationStageHistoryRepository {
    List<ApplicationStageHistory> findByApplicationIdOrderByCreatedAtDesc(UUID applicationId);
    ApplicationStageHistory save(ApplicationStageHistory history);
}
