package com.vthr.erp_hrm.infrastructure.controller.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminVerifyCompanyRequest {
    @NotNull
    private Boolean verified;
}

