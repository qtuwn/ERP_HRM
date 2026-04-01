package com.vthr.erp_hrm.infrastructure.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MessageRequest {
    @NotBlank(message = "Content cannot be empty")
    @Size(max = 2000, message = "Content must be at most 2000 characters")
    private String content;
}
