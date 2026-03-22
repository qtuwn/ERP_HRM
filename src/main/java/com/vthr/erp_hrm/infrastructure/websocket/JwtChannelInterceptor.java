package com.vthr.erp_hrm.infrastructure.websocket;

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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Slf4j
@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final SecretKey secretKey;

    public JwtChannelInterceptor(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = null;

            // Attempt to fetch from Authorization header in STOMP packet payload
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
        return message;
    }
}
