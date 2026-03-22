package com.vthr.erp_hrm.infrastructure.controller.request;

import com.vthr.erp_hrm.core.model.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserRoleRequest {
    @NotNull
    private Role role;
}
