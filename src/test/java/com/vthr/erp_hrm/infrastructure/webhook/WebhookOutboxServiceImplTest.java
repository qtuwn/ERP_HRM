package com.vthr.erp_hrm.infrastructure.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vthr.erp_hrm.infrastructure.persistence.entity.WebhookOutboxEntity;
import com.vthr.erp_hrm.infrastructure.persistence.repository.WebhookOutboxJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class WebhookOutboxServiceImplTest {

    @Mock
    private WebhookOutboxJpaRepository repo;

    private WebhookOutboxServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        service = new WebhookOutboxServiceImpl(repo, new ObjectMapper(), clock);
    }

    @Test
    void enqueueRawJson_shouldNoopWhenDisabled() {
        ReflectionTestUtils.setField(service, "enabled", false);
        service.enqueueRawJson("application:applied", "{\"applicationId\":\"x\"}");
        verify(repo, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void enqueueRawJson_shouldSavePendingWhenEnabled() {
        ReflectionTestUtils.setField(service, "enabled", true);

        service.enqueueRawJson("application:applied", "{\"applicationId\":\"x\"}");

        ArgumentCaptor<WebhookOutboxEntity> cap = ArgumentCaptor.forClass(WebhookOutboxEntity.class);
        verify(repo).save(cap.capture());

        WebhookOutboxEntity saved = cap.getValue();
        assertNotNull(saved.getId());
        assertEquals("application:applied", saved.getEventType());
        assertEquals("{\"applicationId\":\"x\"}", saved.getPayloadJson());
        assertEquals(WebhookOutboxStatus.PENDING, saved.getStatus());
        assertEquals(0, saved.getAttempts());
        assertNotNull(saved.getNextAttemptAt());
    }

    @Test
    void enqueueForApplicationApplied_shouldUseEventType() {
        ReflectionTestUtils.setField(service, "enabled", true);
        service.enqueueForApplicationApplied(UUID.randomUUID());
        verify(repo).save(org.mockito.ArgumentMatchers.argThat(e -> "application:applied".equals(e.getEventType())));
    }
}

