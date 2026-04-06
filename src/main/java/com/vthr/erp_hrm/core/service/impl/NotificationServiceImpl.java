package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.Notification;
import com.vthr.erp_hrm.core.model.NotificationType;
import com.vthr.erp_hrm.core.service.NotificationService;
import com.vthr.erp_hrm.infrastructure.persistence.mapper.NotificationMapper;
import com.vthr.erp_hrm.infrastructure.persistence.repository.NotificationJpaRepository;
import com.vthr.erp_hrm.infrastructure.websocket.RealtimeEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationJpaRepository notificationJpaRepository;
    private final NotificationMapper notificationMapper;
    private final RealtimeEventService realtimeEventService;

    @Override
    @Transactional
    public Notification create(UUID userId, NotificationType type, String title, String body, String link, String metadataJson) {
        if (userId == null) {
            throw new RuntimeException("User not found");
        }
        String t = title != null ? title.trim() : "";
        if (t.isBlank()) {
            t = "Thông báo";
        }
        Notification toSave = Notification.builder()
                .userId(userId)
                .type(type != null ? type : NotificationType.OTHER)
                .title(t)
                .body(body)
                .link(link)
                .metadata(metadataJson)
                .build();
        Notification saved = notificationMapper.toDomain(notificationJpaRepository.save(notificationMapper.toEntity(toSave)));

        // Realtime: push ngay cho user đó (nếu đang online)
        realtimeEventService.emitUserEvent(userId, "notification:new", saved);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Notification> listForUser(UUID userId, Pageable pageable) {
        return notificationJpaRepository.findForUserOrderUnreadFirst(userId, pageable).map(notificationMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return notificationJpaRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Override
    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        if (!notificationJpaRepository.existsByIdAndUserId(notificationId, userId)) {
            throw new RuntimeException("Notification not found");
        }
        notificationJpaRepository.markRead(userId, notificationId, ZonedDateTime.now());
    }

    @Override
    @Transactional
    public void markAllRead(UUID userId) {
        notificationJpaRepository.markAllRead(userId, ZonedDateTime.now());
    }
}

