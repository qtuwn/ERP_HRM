package com.vthr.erp_hrm.infrastructure.controller.response;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
public class ApplicationTaskAttachmentResponse {
    private UUID id;
    private String downloadUrl;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private ZonedDateTime createdAt;
}
