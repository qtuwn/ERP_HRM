package com.vthr.erp_hrm.infrastructure.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateHrMemberRoleRequest {
    @NotBlank(message = "Member role is required")
    private String memberRole;
}
