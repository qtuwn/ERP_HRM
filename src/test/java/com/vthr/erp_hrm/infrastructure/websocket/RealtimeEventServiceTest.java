package com.vthr.erp_hrm.infrastructure.websocket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RealtimeEventServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private RealtimeEventService realtimeEventService;

    @Test
    void emitJobEvent_sendsToExpectedTopicWithEnvelope() {
        UUID jobId = UUID.randomUUID();
        Map<String, Object> payload = Map.of("k", "v");

        realtimeEventService.emitJobEvent(jobId, "application:stage_changed", payload);

        ArgumentCaptor<Object> cap = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/jobs/" + jobId), cap.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> msg = (Map<String, Object>) cap.getValue();
        assertEquals("application:stage_changed", msg.get("type"));
        assertEquals(payload, msg.get("payload"));
        assertNotNull(msg.get("timestamp"));
    }

    @Test
    void emitApplicationEvent_sendsToExpectedTopicWithEnvelope() {
        UUID appId = UUID.randomUUID();
        Map<String, Object> payload = Map.of("x", 1);

        realtimeEventService.emitApplicationEvent(appId, "chat:new_message", payload);

        ArgumentCaptor<Object> cap = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/applications/" + appId), cap.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> msg = (Map<String, Object>) cap.getValue();
        assertEquals("chat:new_message", msg.get("type"));
        assertEquals(payload, msg.get("payload"));
        assertNotNull(msg.get("timestamp"));
    }
}

