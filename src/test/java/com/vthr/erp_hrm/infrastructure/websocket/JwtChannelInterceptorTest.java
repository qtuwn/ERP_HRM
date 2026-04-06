package com.vthr.erp_hrm.infrastructure.websocket;

import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.service.ApplicationAccessService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class JwtChannelInterceptorTest {

    @Mock
    private ApplicationAccessService applicationAccessService;

    private static final String SECRET = "01234567890123456789012345678901"; // 32 bytes for HS256

    private JwtChannelInterceptor interceptor() {
        return new JwtChannelInterceptor(SECRET, applicationAccessService);
    }

    private String jwt(String sub, String role) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(sub)
                .claim("role", role)
                .issuedAt(new Date())
                .signWith(key)
                .compact();
    }

    @Test
    void connect_shouldSetUserWhenJwtValid() {
        UUID userId = UUID.randomUUID();
        String token = jwt(userId.toString(), "HR");

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        Message<byte[]> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> res = interceptor().preSend(msg, null);

        StompHeaderAccessor out = StompHeaderAccessor.wrap(res);
        assertNotNull(out.getUser());
        assertEquals(userId.toString(), out.getUser().getName());
        Authentication auth = (Authentication) out.getUser();
        assertEquals(1, auth.getAuthorities().size());
        assertEquals("ROLE_HR", auth.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void subscribe_shouldAllowApplicationTopicWhenParticipant() {
        UUID userId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userId.toString(),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
        );

        doNothing().when(applicationAccessService).requireParticipantForMessaging(eq(userId), eq(Role.CANDIDATE), eq(appId));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/applications/" + appId);
        accessor.setUser(auth);
        Message<byte[]> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> res = interceptor().preSend(msg, null);

        assertNotNull(res);
        verify(applicationAccessService).requireParticipantForMessaging(eq(userId), eq(Role.CANDIDATE), eq(appId));
    }

    @Test
    void subscribe_shouldAllowJobTopicWhenRecruiterAllowed() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userId.toString(),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_HR"))
        );

        doNothing().when(applicationAccessService).requireRecruiterForJobTopic(eq(userId), eq(Role.HR), eq(jobId));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/jobs/" + jobId);
        accessor.setUser(auth);
        Message<byte[]> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> res = interceptor().preSend(msg, null);

        assertNotNull(res);
        verify(applicationAccessService).requireRecruiterForJobTopic(eq(userId), eq(Role.HR), eq(jobId));
    }

    @Test
    void subscribe_shouldDenyWhenUnsupportedDestination() {
        UUID userId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userId.toString(),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_HR"))
        );

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/unknown/abc");
        accessor.setUser(auth);
        Message<byte[]> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> res = interceptor().preSend(msg, null);

        assertNull(res);
        verify(applicationAccessService, never()).requireParticipantForMessaging(any(), any(), any());
        verify(applicationAccessService, never()).requireRecruiterForJobTopic(any(), any(), any());
    }

    @Test
    void subscribe_shouldDenyWhenAccessServiceThrows() {
        UUID userId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userId.toString(),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
        );

        doThrow(new RuntimeException("Access denied"))
                .when(applicationAccessService)
                .requireParticipantForMessaging(eq(userId), eq(Role.CANDIDATE), eq(appId));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/applications/" + appId);
        accessor.setUser(auth);
        Message<byte[]> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> res = interceptor().preSend(msg, null);

        assertNull(res);
        verify(applicationAccessService).requireParticipantForMessaging(eq(userId), eq(Role.CANDIDATE), eq(appId));
    }
}

