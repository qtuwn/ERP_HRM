package com.vthr.erp_hrm.infrastructure.controller.request;

import lombok.Data;
import java.time.ZonedDateTime;

@Data
public class JobUpdateRequest {
    private String title;
    private String description;
    private String department;
    private String requiredSkills;
    private ZonedDateTime expiresAt;
}
