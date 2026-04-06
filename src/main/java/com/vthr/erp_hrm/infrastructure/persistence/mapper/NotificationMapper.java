package com.vthr.erp_hrm.infrastructure.persistence.mapper;

import com.vthr.erp_hrm.core.model.Notification;
import com.vthr.erp_hrm.core.model.NotificationType;
import com.vthr.erp_hrm.infrastructure.persistence.entity.NotificationEntity;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public Notification toDomain(NotificationEntity entity) {
        if (entity == null) return null;
        return Notification.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .type(entity.getType() != null ? NotificationType.valueOf(entity.getType()) : null)
                .title(entity.getTitle())
                .body(entity.getBody())
                .link(entity.getLink())
                .metadata(entity.getMetadata())
                .readAt(entity.getReadAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public NotificationEntity toEntity(Notification domain) {
        if (domain == null) return null;
        return NotificationEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .type(domain.getType() != null ? domain.getType().name() : NotificationType.OTHER.name())
                .title(domain.getTitle())
                .body(domain.getBody())
                .link(domain.getLink())
                .metadata(domain.getMetadata())
                .readAt(domain.getReadAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}

