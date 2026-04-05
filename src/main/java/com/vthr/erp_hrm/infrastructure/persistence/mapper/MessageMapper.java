package com.vthr.erp_hrm.infrastructure.persistence.mapper;

import com.vthr.erp_hrm.core.model.Message;
import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.infrastructure.persistence.entity.MessageEntity;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    public Message toDomain(MessageEntity entity) {
        if (entity == null)
            return null;
        return Message.builder()
                .id(entity.getId())
                .applicationId(entity.getApplicationId())
                .senderId(entity.getSenderId())
                .senderRole(Role.fromString(entity.getSenderRole()))
                .content(entity.getContent())
                .readAt(entity.getReadAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public MessageEntity toEntity(Message domain) {
        if (domain == null)
            return null;
        return MessageEntity.builder()
                .id(domain.getId())
                .applicationId(domain.getApplicationId())
                .senderId(domain.getSenderId())
                .senderRole(domain.getSenderRole() != null ? domain.getSenderRole().name() : null)
                .content(domain.getContent())
                .readAt(domain.getReadAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
