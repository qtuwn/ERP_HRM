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
public class Notification {
    private UUID id;
    private UUID userId;
    private NotificationType type;
    private String title;
    private String body;
    private String link;
    /** JSON string (optional) */
    private String metadata;
    private ZonedDateTime readAt;
    private ZonedDateTime createdAt;
}

