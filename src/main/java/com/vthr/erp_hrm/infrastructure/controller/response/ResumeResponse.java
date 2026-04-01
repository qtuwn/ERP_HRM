package com.vthr.erp_hrm.infrastructure.controller.response;

import com.vthr.erp_hrm.core.model.Resume;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
public class ResumeResponse {
    private UUID id;
    private String title;
    private boolean isDefault;
    private String downloadUrl;
    private String originalFilename;
    private String mimeType;
    private Long sizeBytes;
    private ZonedDateTime createdAt;

    public static ResumeResponse fromDomain(Resume r, String downloadUrl) {
        if (r == null) {
            return null;
        }
        return ResumeResponse.builder()
                .id(r.getId())
                .title(r.getTitle())
                .isDefault(r.isDefault())
                .downloadUrl(downloadUrl)
                .originalFilename(r.getOriginalFilename())
                .mimeType(r.getMimeType())
                .sizeBytes(r.getSizeBytes())
                .createdAt(r.getCreatedAt())
                .build();
    }
}

