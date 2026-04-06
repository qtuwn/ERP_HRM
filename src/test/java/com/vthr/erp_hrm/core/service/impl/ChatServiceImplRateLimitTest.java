package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.Application;
import com.vthr.erp_hrm.core.model.Message;
import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.repository.ApplicationRepository;
import com.vthr.erp_hrm.core.repository.MessageRepository;
import com.vthr.erp_hrm.core.repository.UserRepository;
import com.vthr.erp_hrm.core.service.ApplicationAccessService;
import com.vthr.erp_hrm.core.service.ChatRateLimitService;
import com.vthr.erp_hrm.infrastructure.email.EmailQueueService;
import com.vthr.erp_hrm.infrastructure.websocket.RealtimeEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplRateLimitTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationAccessService applicationAccessService;

    @Mock
    private RealtimeEventService realtimeEventService;

    @Mock
    private EmailQueueService emailQueueService;

    @Mock
    private UserRepository userRepository;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));

    private final ChatRateLimitService chatRateLimitService = new InMemoryChatRateLimitService(clock, 3, 10, 1000);

    private ChatServiceImpl chatService;

    @BeforeEach
    void setup() {
        this.chatService = new ChatServiceImpl(
                messageRepository,
                applicationRepository,
                applicationAccessService,
                realtimeEventService,
                emailQueueService,
                userRepository,
                chatRateLimitService
        );
    }

    @Test
    void sendMessage_shouldRateLimitPerUserPerApplication() {
        UUID appId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();

        doNothing().when(applicationAccessService).requireParticipantForMessaging(senderId, Role.CANDIDATE, appId);
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(Application.builder()
                .id(appId)
                .candidateId(senderId)
                .build()));

        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            return Message.builder()
                    .id(UUID.randomUUID())
                    .applicationId(m.getApplicationId())
                    .senderId(m.getSenderId())
                    .senderRole(m.getSenderRole())
                    .content(m.getContent())
                    .createdAt(m.getCreatedAt())
                    .build();
        });

        chatService.sendMessage(appId, senderId, Role.CANDIDATE, "m1");
        chatService.sendMessage(appId, senderId, Role.CANDIDATE, "m2");
        chatService.sendMessage(appId, senderId, Role.CANDIDATE, "m3");

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                chatService.sendMessage(appId, senderId, Role.CANDIDATE, "m4")
        );
        assertEquals("Rate limit exceeded", ex.getMessage());

        verify(realtimeEventService, times(3)).emitApplicationEvent(eq(appId), eq("chat:new_message"), any());
    }

    @Test
    void indicateTyping_shouldBeThrottled() {
        UUID appId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();

        doNothing().when(applicationAccessService).requireParticipantForMessaging(senderId, Role.CANDIDATE, appId);

        chatService.indicateTyping(appId, senderId, Role.CANDIDATE);
        chatService.indicateTyping(appId, senderId, Role.CANDIDATE);

        verify(realtimeEventService, times(1)).emitApplicationEvent(eq(appId), eq("chat:typing"), any());

        clock.advanceSeconds(2);
        chatService.indicateTyping(appId, senderId, Role.CANDIDATE);
        verify(realtimeEventService, times(2)).emitApplicationEvent(eq(appId), eq("chat:typing"), any());
    }

    @Test
    void sendMessage_blankContent_shouldThrowBeforeSave() {
        UUID appId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();

        doNothing().when(applicationAccessService).requireParticipantForMessaging(senderId, Role.CANDIDATE, appId);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                chatService.sendMessage(appId, senderId, Role.CANDIDATE, "   ")
        );
        assertEquals("Content cannot be empty", ex.getMessage());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void sendMessage_tooLong_shouldThrowBeforeSave() {
        UUID appId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        String longContent = "x".repeat(2001);

        doNothing().when(applicationAccessService).requireParticipantForMessaging(senderId, Role.CANDIDATE, appId);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                chatService.sendMessage(appId, senderId, Role.CANDIDATE, longContent)
        );
        assertEquals("Message too long", ex.getMessage());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void sendMessage_shouldStillEmitRealtimePayloadThatContainsSavedMessage() {
        UUID appId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();

        doNothing().when(applicationAccessService).requireParticipantForMessaging(senderId, Role.CANDIDATE, appId);
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(Application.builder()
                .id(appId)
                .candidateId(senderId)
                .build()));

        UUID savedId = UUID.randomUUID();
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            return Message.builder()
                    .id(savedId)
                    .applicationId(m.getApplicationId())
                    .senderId(m.getSenderId())
                    .senderRole(m.getSenderRole())
                    .content(m.getContent())
                    .createdAt(m.getCreatedAt())
                    .build();
        });

        chatService.sendMessage(appId, senderId, Role.CANDIDATE, "hello");

        ArgumentCaptor<Object> cap = ArgumentCaptor.forClass(Object.class);
        verify(realtimeEventService).emitApplicationEvent(eq(appId), eq("chat:new_message"), cap.capture());
        Object payload = cap.getValue();
        assertNotNull(payload);
        verify(emailQueueService, never()).enqueueEmail(any(), any(), any(), any());
    }

    static class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advanceSeconds(long seconds) {
            this.instant = this.instant.plusSeconds(seconds);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}

