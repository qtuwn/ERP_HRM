package com.vthr.erp_hrm.infrastructure.websocket;

import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.service.ApplicationAccessService;
import com.vthr.erp_hrm.infrastructure.security.SecurityRoleResolver;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private static final Pattern TOPIC_APPLICATION = Pattern.compile("^/topic/applications/([0-9a-fA-F-]{36})$");
    private static final Pattern TOPIC_JOB = Pattern.compile("^/topic/jobs/([0-9a-fA-F-]{36})$");
    private static final Pattern TOPIC_NOTIFICATIONS = Pattern.compile("^/topic/notifications/([0-9a-fA-F-]{36})$");

    private final SecretKey secretKey;
    private final ApplicationAccessService applicationAccessService;

    public JwtChannelInterceptor(
            @Value("${jwt.secret}") String secret,
            ApplicationAccessService applicationAccessService) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.applicationAccessService = applicationAccessService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = null;

            String rawToken = accessor.getFirstNativeHeader("Authorization");
            if (rawToken != null && rawToken.startsWith("Bearer ")) {
                token = rawToken.substring(7);
            }

            if (token != null) {
                try {
                    Claims claims = Jwts.parser()
                            .verifyWith(secretKey)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();

                    String userId = claims.getSubject();
                    String role = claims.get("role", String.class);

                    if (userId != null) {
                        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userId, null, Collections.singletonList(authority));
                        accessor.setUser(authToken);
                        log.debug("WebSocket STOMP connected User: {}", userId);
                    }
                } catch (Exception e) {
                    log.error("WebSocket STOMP Connection JWT Verification Failed: {}", e.getMessage());
                }
            } else {
                log.warn("STOMP CONNECT missing or invalid Authorization header");
            }
        }

        if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String dest = accessor.getDestination();
            if (dest != null) {
                Authentication auth = (Authentication) accessor.getUser();
                if (auth == null || !auth.isAuthenticated()) {
                    log.warn("STOMP SUBSCRIBE denied (no auth): {}", dest);
                    return null;
                }
                UUID uid;
                try {
                    uid = UUID.fromString(auth.getName());
                } catch (Exception e) {
                    log.warn("STOMP SUBSCRIBE denied (bad user id): {}", dest);
                    return null;
                }
                Role role = SecurityRoleResolver.resolveRole(auth);
                try {
                    Matcher mApp = TOPIC_APPLICATION.matcher(dest);
                    if (mApp.matches()) {
                        applicationAccessService.requireParticipantForMessaging(uid, role, UUID.fromString(mApp.group(1)));
                        return message;
                    }
                    Matcher mJob = TOPIC_JOB.matcher(dest);
                    if (mJob.matches()) {
                        applicationAccessService.requireRecruiterForJobTopic(uid, role, UUID.fromString(mJob.group(1)));
                        return message;
                    }
                    Matcher mNoti = TOPIC_NOTIFICATIONS.matcher(dest);
                    if (mNoti.matches()) {
                        UUID targetUserId = UUID.fromString(mNoti.group(1));
                        if (!uid.equals(targetUserId) && role != Role.ADMIN) {
                            throw new RuntimeException("Access denied");
                        }
                        return message;
                    }
                    log.warn("STOMP SUBSCRIBE denied (unsupported destination): {}", dest);
                    return null;
                } catch (RuntimeException ex) {
                    log.warn("STOMP SUBSCRIBE denied {}: {}", dest, ex.getMessage());
                    return null;
                }
            }
        }

        return message;
    }
}
