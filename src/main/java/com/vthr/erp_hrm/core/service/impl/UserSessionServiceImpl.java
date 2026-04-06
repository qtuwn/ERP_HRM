package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.RefreshToken;
import com.vthr.erp_hrm.core.model.UserSessionItem;
import com.vthr.erp_hrm.core.repository.RefreshTokenRepository;
import com.vthr.erp_hrm.core.service.UserSessionService;
import com.vthr.erp_hrm.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSessionServiceImpl implements UserSessionService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Override
    public List<UserSessionItem> listSessions(UUID userId) {
        ZonedDateTime now = ZonedDateTime.now();
        return refreshTokenRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(t -> toItem(t, now))
                .toList();
    }

    @Override
    public Optional<UUID> findSessionIdForRefreshToken(UUID userId, String refreshTokenRaw) {
        if (refreshTokenRaw == null || refreshTokenRaw.isBlank()) {
            return Optional.empty();
        }
        String hash = jwtService.hashToken(refreshTokenRaw.trim());
        return refreshTokenRepository.findByTokenHash(hash)
                .filter(t -> t.getUserId().equals(userId))
                .map(RefreshToken::getId);
    }

    @Override
    @Transactional
    public boolean revokeSession(UUID userId, UUID sessionId) {
        return refreshTokenRepository.revokeByIdForUser(sessionId, userId) > 0;
    }

    @Override
    @Transactional
    public void revokeAllSessions(UUID userId) {
        refreshTokenRepository.revokeAllUserTokens(userId);
    }

    private static UserSessionItem toItem(RefreshToken t, ZonedDateTime now) {
        boolean stillValid = !t.isRevoked() && t.getExpiresAt() != null && t.getExpiresAt().isAfter(now);
        return UserSessionItem.builder()
                .id(t.getId())
                .createdAt(t.getCreatedAt())
                .expiresAt(t.getExpiresAt())
                .revoked(t.isRevoked())
                .stillValid(stillValid)
                .clientIp(t.getClientIp())
                .userAgent(t.getUserAgent())
                .build();
    }
}
