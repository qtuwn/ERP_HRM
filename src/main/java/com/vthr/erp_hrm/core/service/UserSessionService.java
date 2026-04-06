package com.vthr.erp_hrm.core.service;

import com.vthr.erp_hrm.core.model.UserSessionItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionService {
    List<UserSessionItem> listSessions(UUID userId);

    Optional<UUID> findSessionIdForRefreshToken(UUID userId, String refreshTokenRaw);

    /** @return false nếu không tìm thấy phiên hoặc đã thu hồi */
    boolean revokeSession(UUID userId, UUID sessionId);

    void revokeAllSessions(UUID userId);
}
