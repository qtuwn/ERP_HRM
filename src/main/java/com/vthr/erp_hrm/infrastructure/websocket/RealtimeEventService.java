package com.vthr.erp_hrm.infrastructure.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeEventService {

    private final SimpMessagingTemplate messagingTemplate;

    public void emitJobEvent(UUID jobId, String eventType, Object payload) {
        String destination = "/topic/jobs/" + jobId.toString();
        Map<String, Object> message = Map.of(
                "type", eventType,
                "payload", payload,
                "timestamp", ZonedDateTime.now().toString()
        );
        try {
            messagingTemplate.convertAndSend(destination, (Object) message);
            log.debug("Emitted realtime event {} to {}", eventType, destination);
        } catch (Exception e) {
            log.error("Failed to emit STOMP event: {}", e.getMessage(), e);
        }
    }

    public void emitApplicationEvent(UUID applicationId, String eventType, Object payload) {
        String destination = "/topic/applications/" + applicationId.toString();
        Map<String, Object> message = Map.of(
                "type", eventType,
                "payload", payload,
                "timestamp", ZonedDateTime.now().toString()
        );
        try {
            messagingTemplate.convertAndSend(destination, (Object) message);
            log.debug("Emitted realtime event {} to {}", eventType, destination);
        } catch (Exception e) {
            log.error("Failed to emit STOMP event: {}", e.getMessage(), e);
        }
    }
}
