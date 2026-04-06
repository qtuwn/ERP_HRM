package com.vthr.erp_hrm.core.service;

import java.util.UUID;

public interface ChatRateLimitService {
    void assertCanSendMessage(UUID applicationId, UUID senderId);

    boolean shouldAllowTyping(UUID applicationId, UUID senderId);
}

