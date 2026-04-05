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
public class Resume {
    private UUID id;
    private UUID userId;
    private String title;
    private String storagePath;
    private String originalFilename;
    private String mimeType;
    private Long sizeBytes;
    private boolean isDefault;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}

