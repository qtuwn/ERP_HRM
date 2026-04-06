package com.vthr.erp_hrm.infrastructure.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vthr.erp_hrm.infrastructure.persistence.entity.WebhookOutboxEntity;
import com.vthr.erp_hrm.infrastructure.persistence.repository.WebhookOutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookOutboxServiceImpl implements WebhookOutboxService {
    private final WebhookOutboxJpaRepository repo;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Value("${app.webhook.enabled:false}")
    private boolean enabled;

    @Override
    public void enqueue(String eventType, Object payload) {
        try {
            enqueueRawJson(eventType, objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize webhook payload for eventType='{}': {}", eventType, e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void enqueueRawJson(String eventType, String payloadJson) {
        if (!enabled) {
            return;
        }
        WebhookOutboxEntity entity = WebhookOutboxEntity.builder()
                .id(UUID.randomUUID())
                .eventType(eventType)
                .payloadJson(payloadJson)
                .status(WebhookOutboxStatus.PENDING)
                .attempts(0)
                .nextAttemptAt(ZonedDateTime.now(clock))
                .build();
        repo.save(entity);
    }

    @Override
    public void enqueueForApplicationApplied(UUID applicationId) {
        enqueue("application:applied", Map.of("applicationId", applicationId));
    }

    @Override
    public void enqueueForApplicationRejected(UUID applicationId) {
        enqueue("application:rejected", Map.of("applicationId", applicationId));
    }
}

