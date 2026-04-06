package com.vthr.erp_hrm.infrastructure.controller.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class BulkRejectRequest {
    @NotEmpty(message = "applicationIds is required")
    private List<UUID> applicationIds;
}
