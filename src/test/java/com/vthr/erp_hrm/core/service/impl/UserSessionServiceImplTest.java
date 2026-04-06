package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.RefreshToken;
import com.vthr.erp_hrm.core.model.UserSessionItem;
import com.vthr.erp_hrm.core.repository.RefreshTokenRepository;
import com.vthr.erp_hrm.infrastructure.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSessionServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserSessionServiceImpl userSessionService;

    @Test
    void listSessions_marksStillValid() {
        UUID uid = UUID.randomUUID();
        ZonedDateTime future = ZonedDateTime.now().plusDays(1);
        RefreshToken t = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(uid)
                .tokenHash("h")
                .expiresAt(future)
                .revoked(false)
                .createdAt(ZonedDateTime.now().minusHours(1))
                .build();
        when(refreshTokenRepository.findByUserIdOrderByCreatedAtDesc(uid)).thenReturn(List.of(t));

        List<UserSessionItem> items = userSessionService.listSessions(uid);

        assertEquals(1, items.size());
        assertTrue(items.get(0).isStillValid());
        assertFalse(items.get(0).isRevoked());
    }

    @Test
    void findSessionIdForRefreshToken_matchesHash() {
        UUID uid = UUID.randomUUID();
        UUID sid = UUID.randomUUID();
        when(jwtService.hashToken("raw")).thenReturn("hh");
        when(refreshTokenRepository.findByTokenHash("hh"))
                .thenReturn(Optional.of(RefreshToken.builder().id(sid).userId(uid).build()));

        Optional<UUID> out = userSessionService.findSessionIdForRefreshToken(uid, "raw");

        assertEquals(Optional.of(sid), out);
    }

    @Test
    void findSessionIdForRefreshToken_wrongUser_empty() {
        UUID uid = UUID.randomUUID();
        when(jwtService.hashToken("raw")).thenReturn("hh");
        when(refreshTokenRepository.findByTokenHash("hh"))
                .thenReturn(Optional.of(RefreshToken.builder().id(UUID.randomUUID()).userId(UUID.randomUUID()).build()));

        assertTrue(userSessionService.findSessionIdForRefreshToken(uid, "raw").isEmpty());
    }

    @Test
    void revokeSession_delegatesToRepository() {
        UUID uid = UUID.randomUUID();
        UUID sid = UUID.randomUUID();
        when(refreshTokenRepository.revokeByIdForUser(sid, uid)).thenReturn(1);

        assertTrue(userSessionService.revokeSession(uid, sid));
    }

    @Test
    void revokeAllSessions_delegates() {
        UUID uid = UUID.randomUUID();
        userSessionService.revokeAllSessions(uid);
        verify(refreshTokenRepository).revokeAllUserTokens(eq(uid));
    }
}
