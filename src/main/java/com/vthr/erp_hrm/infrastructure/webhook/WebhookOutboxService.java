package com.vthr.erp_hrm.infrastructure.webhook;

import java.util.UUID;

public interface WebhookOutboxService {
    void enqueue(String eventType, Object payload);

    void enqueueRawJson(String eventType, String payloadJson);

    void enqueueForApplicationApplied(UUID applicationId);

    void enqueueForApplicationRejected(UUID applicationId);
}

