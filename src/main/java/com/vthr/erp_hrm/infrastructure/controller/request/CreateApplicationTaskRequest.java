package com.vthr.erp_hrm.infrastructure.controller.request;

import com.vthr.erp_hrm.core.model.ApplicationTaskDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class CreateApplicationTaskRequest {
    @NotBlank
    private String title;
    private String description;
    @NotNull
    private ApplicationTaskDocumentType documentType;
    private ZonedDateTime dueAt;
}
