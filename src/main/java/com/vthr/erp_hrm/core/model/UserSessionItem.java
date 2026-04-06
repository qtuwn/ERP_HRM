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
public class UserSessionItem {
    private UUID id;
    private ZonedDateTime createdAt;
    private ZonedDateTime expiresAt;
    private boolean revoked;
    /** Còn hiệu lực (chưa revoke và chưa hết hạn theo server). */
    private boolean stillValid;

    private String clientIp;
    private String userAgent;
}
