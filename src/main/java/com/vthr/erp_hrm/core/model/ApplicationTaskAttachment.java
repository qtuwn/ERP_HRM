package com.vthr.erp_hrm.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationTaskAttachment {
    private UUID id;
    private UUID taskId;
    /** Đường dẫn tương đối trong thư mục task-docs (applicationId/taskId/file). */
    private String storagePath;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private UUID uploadedByUserId;
    private ZonedDateTime createdAt;
}
