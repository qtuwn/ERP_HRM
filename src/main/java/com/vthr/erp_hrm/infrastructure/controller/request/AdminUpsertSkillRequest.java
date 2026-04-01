package com.vthr.erp_hrm.infrastructure.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class AdminUpsertSkillRequest {
    private UUID categoryId;

    @NotBlank
    @Size(max = 255)
    private String name;
}

