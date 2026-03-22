package com.vthr.erp_hrm.infrastructure.controller.request;

import com.vthr.erp_hrm.core.model.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApplicationUpdateRequest {
    @NotNull
    private ApplicationStatus status;
    private String note;
}
