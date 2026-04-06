package com.vthr.erp_hrm.infrastructure.controller.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserDepartmentRequest {
    @Size(max = 255, message = "Department name is too long")
    private String department;
}
