package com.vthr.erp_hrm.infrastructure.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResendVerificationRequest {
    @NotBlank
    @Email
    private String email;
}
