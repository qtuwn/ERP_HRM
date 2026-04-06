package com.vthr.erp_hrm.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationTask {
    private UUID id;
    private UUID applicationId;
    private String title;
    private String description;
    private ApplicationTaskDocumentType documentType;
    private ApplicationTaskStatus status;
    private String hrFeedback;
    private ZonedDateTime dueAt;
    private UUID createdByUserId;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    @Builder.Default
    private List<ApplicationTaskAttachment> attachments = new ArrayList<>();
}
