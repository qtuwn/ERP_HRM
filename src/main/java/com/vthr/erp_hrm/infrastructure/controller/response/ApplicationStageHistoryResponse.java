package com.vthr.erp_hrm.infrastructure.controller.response;

import com.vthr.erp_hrm.core.model.ApplicationStageHistory;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
public class ApplicationStageHistoryResponse {
    private UUID id;
    private String fromStage;
    private String toStage;
    private UUID changedBy;
    private String note;
    private ZonedDateTime createdAt;

    public static ApplicationStageHistoryResponse fromDomain(ApplicationStageHistory h) {
        if (h == null) {
            return null;
        }
        return ApplicationStageHistoryResponse.builder()
                .id(h.getId())
                .fromStage(h.getFromStage() != null ? h.getFromStage().name() : null)
                .toStage(h.getToStage() != null ? h.getToStage().name() : null)
                .changedBy(h.getChangedBy())
                .note(h.getNote())
                .createdAt(h.getCreatedAt())
                .build();
    }
}
