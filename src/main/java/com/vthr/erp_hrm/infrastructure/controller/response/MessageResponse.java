package com.vthr.erp_hrm.infrastructure.controller.response;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
public class MessageResponse {
    private UUID id;
    private UUID applicationId;
    private UUID senderId;
    private String senderRole;
    private String content;
    private ZonedDateTime readAt;
    private ZonedDateTime createdAt;
}
