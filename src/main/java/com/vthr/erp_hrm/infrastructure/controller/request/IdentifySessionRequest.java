package com.vthr.erp_hrm.infrastructure.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IdentifySessionRequest {
    @NotBlank(message = "refreshToken is required")
    private String refreshToken;
}
