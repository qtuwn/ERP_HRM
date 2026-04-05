package com.vthr.erp_hrm.core.model;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
public class Message {
    private UUID id;
    private UUID applicationId;
    private UUID senderId;
    private Role senderRole;
    private String content;
    private ZonedDateTime readAt;
    private ZonedDateTime createdAt;
}
