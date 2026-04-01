package com.vthr.erp_hrm.infrastructure.controller.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ApplyWithResumeRequest {
    @NotNull
    private UUID resumeId;
}

