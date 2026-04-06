package com.vthr.erp_hrm.infrastructure.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class AddHrMemberRequest {
    private UUID userId;

    @NotBlank(message = "Member role is required")
    private String memberRole;
}
