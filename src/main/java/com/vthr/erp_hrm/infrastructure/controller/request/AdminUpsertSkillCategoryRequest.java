package com.vthr.erp_hrm.infrastructure.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUpsertSkillCategoryRequest {
    @NotBlank
    @Size(max = 255)
    private String name;
}

