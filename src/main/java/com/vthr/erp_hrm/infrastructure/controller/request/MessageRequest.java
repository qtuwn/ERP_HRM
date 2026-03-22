package com.vthr.erp_hrm.infrastructure.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MessageRequest {
    @NotBlank(message = "Content cannot be empty")
    private String content;
}
