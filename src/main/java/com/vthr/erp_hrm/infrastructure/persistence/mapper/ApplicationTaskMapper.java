package com.vthr.erp_hrm.infrastructure.persistence.mapper;

import com.vthr.erp_hrm.core.model.ApplicationTask;
import com.vthr.erp_hrm.core.model.ApplicationTaskAttachment;
import com.vthr.erp_hrm.core.model.ApplicationTaskDocumentType;
import com.vthr.erp_hrm.core.model.ApplicationTaskStatus;
import com.vthr.erp_hrm.infrastructure.persistence.entity.ApplicationTaskAttachmentEntity;
import com.vthr.erp_hrm.infrastructure.persistence.entity.ApplicationTaskEntity;

import java.util.stream.Collectors;

public final class ApplicationTaskMapper {

    private ApplicationTaskMapper() {
    }

    public static ApplicationTask toDomain(ApplicationTaskEntity e, boolean includeAttachments) {
        if (e == null) {
            return null;
        }
        ApplicationTask.ApplicationTaskBuilder b = ApplicationTask.builder()
                .id(e.getId())
                .applicationId(e.getApplicationId())
                .title(e.getTitle())
                .description(e.getDescription())
                .documentType(parseDocType(e.getDocumentType()))
                .status(parseStatus(e.getStatus()))
                .hrFeedback(e.getHrFeedback())
                .dueAt(e.getDueAt())
                .createdByUserId(e.getCreatedByUserId())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt());
        if (includeAttachments && e.getAttachments() != null) {
            b.attachments(e.getAttachments().stream()
                    .map(ApplicationTaskMapper::attachmentToDomain)
                    .collect(Collectors.toList()));
        } else {
            b.attachments(java.util.Collections.emptyList());
        }
        return b.build();
    }

    public static ApplicationTaskAttachment attachmentToDomain(ApplicationTaskAttachmentEntity a) {
        if (a == null) {
            return null;
        }
        return ApplicationTaskAttachment.builder()
                .id(a.getId())
                .taskId(a.getTask() != null ? a.getTask().getId() : null)
                .storagePath(a.getStoragePath())
                .originalFilename(a.getOriginalFilename())
                .contentType(a.getContentType())
                .fileSize(a.getFileSize())
                .uploadedByUserId(a.getUploadedByUserId())
                .createdAt(a.getCreatedAt())
                .build();
    }

    public static ApplicationTaskDocumentType parseDocType(String raw) {
        if (raw == null || raw.isBlank()) {
            return ApplicationTaskDocumentType.OTHER;
        }
        try {
            return ApplicationTaskDocumentType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return ApplicationTaskDocumentType.OTHER;
        }
    }

    public static ApplicationTaskStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return ApplicationTaskStatus.OPEN;
        }
        try {
            return ApplicationTaskStatus.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return ApplicationTaskStatus.OPEN;
        }
    }
}
