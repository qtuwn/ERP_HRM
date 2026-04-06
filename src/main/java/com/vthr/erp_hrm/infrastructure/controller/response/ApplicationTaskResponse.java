package com.vthr.erp_hrm.infrastructure.controller.response;

import com.vthr.erp_hrm.core.model.ApplicationTask;
import com.vthr.erp_hrm.core.model.ApplicationTaskAttachment;
import com.vthr.erp_hrm.infrastructure.storage.SignedUrlService;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
public class ApplicationTaskResponse {
    private UUID id;
    private UUID applicationId;
    private String title;
    private String description;
    private String documentType;
    private String status;
    private String hrFeedback;
    private ZonedDateTime dueAt;
    private UUID createdByUserId;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private List<ApplicationTaskAttachmentResponse> attachments;

    public static ApplicationTaskResponse fromDomain(ApplicationTask task, SignedUrlService signedUrlService, boolean withAttachments) {
        List<ApplicationTaskAttachmentResponse> atts = List.of();
        if (withAttachments && task.getAttachments() != null) {
            atts = task.getAttachments().stream()
                    .map(a -> mapAttachment(a, signedUrlService))
                    .collect(Collectors.toList());
        }
        return ApplicationTaskResponse.builder()
                .id(task.getId())
                .applicationId(task.getApplicationId())
                .title(task.getTitle())
                .description(task.getDescription())
                .documentType(task.getDocumentType() != null ? task.getDocumentType().name() : null)
                .status(task.getStatus() != null ? task.getStatus().name() : null)
                .hrFeedback(task.getHrFeedback())
                .dueAt(task.getDueAt())
                .createdByUserId(task.getCreatedByUserId())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .attachments(atts)
                .build();
    }

    private static ApplicationTaskAttachmentResponse mapAttachment(ApplicationTaskAttachment a, SignedUrlService signedUrlService) {
        String url = a.getStoragePath() != null
                ? signedUrlService.generateSignedUrl("/api/files/task-docs", a.getStoragePath())
                : null;
        return ApplicationTaskAttachmentResponse.builder()
                .id(a.getId())
                .downloadUrl(url)
                .originalFilename(a.getOriginalFilename())
                .contentType(a.getContentType())
                .fileSize(a.getFileSize())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
