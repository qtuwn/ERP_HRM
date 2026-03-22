package com.vthr.erp_hrm.infrastructure.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.ZonedDateTime;

@Data
public class JobCreateRequest {
    @NotBlank
    private String title;
    
    @NotBlank
    private String description;
    
    private String department;
    private String requiredSkills;
    private ZonedDateTime expiresAt;
}
