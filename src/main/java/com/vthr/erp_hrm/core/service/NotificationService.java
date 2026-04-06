package com.vthr.erp_hrm.core.service;

import com.vthr.erp_hrm.core.model.Notification;
import com.vthr.erp_hrm.core.model.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {
    Notification create(UUID userId, NotificationType type, String title, String body, String link, String metadataJson);

    Page<Notification> listForUser(UUID userId, Pageable pageable);

    long countUnread(UUID userId);

    void markRead(UUID userId, UUID notificationId);

    void markAllRead(UUID userId);
}

