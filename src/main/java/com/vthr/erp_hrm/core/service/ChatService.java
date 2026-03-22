package com.vthr.erp_hrm.core.service;

import com.vthr.erp_hrm.core.model.Message;
import com.vthr.erp_hrm.core.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ChatService {
    Message sendMessage(UUID applicationId, UUID senderId, Role senderRole, String content);
    Page<Message> getMessageHistory(UUID applicationId, Pageable pageable);
    void indicateTyping(UUID applicationId, UUID senderId, Role senderRole);
}
