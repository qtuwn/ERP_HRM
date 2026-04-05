package com.vthr.erp_hrm.infrastructure.controller.request;

import com.vthr.erp_hrm.core.model.ApplicationStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class BulkStatusUpdateRequest {
    @NotEmpty
    private List<UUID> applicationIds;

    @NotNull
    private ApplicationStatus status;

    private String note;
}

