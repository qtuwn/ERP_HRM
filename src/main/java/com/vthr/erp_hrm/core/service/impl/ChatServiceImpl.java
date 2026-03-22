package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.Application;
import com.vthr.erp_hrm.core.model.Message;
import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.repository.ApplicationRepository;
import com.vthr.erp_hrm.core.repository.MessageRepository;
import com.vthr.erp_hrm.core.service.ChatService;
import com.vthr.erp_hrm.infrastructure.websocket.RealtimeEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.UUID;
import com.vthr.erp_hrm.core.model.User;
import com.vthr.erp_hrm.core.repository.UserRepository;
import com.vthr.erp_hrm.infrastructure.email.EmailQueueService;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final MessageRepository messageRepository;
    private final ApplicationRepository applicationRepository;
    private final RealtimeEventService realtimeEventService;
    private final EmailQueueService emailQueueService;
    private final UserRepository userRepository;

    @Override
    public Message sendMessage(UUID applicationId, UUID senderId, Role senderRole, String content) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (senderRole == Role.CANDIDATE && !application.getCandidateId().equals(senderId)) {
            throw new RuntimeException("Unauthorized: Cannot send messages to other candidates' applications");
        }

        Message message = Message.builder()
                .applicationId(applicationId)
                .senderId(senderId)
                .senderRole(senderRole)
                .content(content)
                .createdAt(ZonedDateTime.now())
                .build();

        Message saved = messageRepository.save(message);
        
        realtimeEventService.emitApplicationEvent(applicationId, "chat:new_message", saved);
        
        if (senderRole != Role.CANDIDATE) {
            User candidate = userRepository.findById(application.getCandidateId()).orElse(null);
            if (candidate != null) {
                emailQueueService.enqueueEmail(
                    candidate.getEmail(),
                    "Tin Nhắn Mới Nhận Trên Hệ Thống ATS",
                    "chat_notification",
                    java.util.Map.of(
                        "recipientName", candidate.getFullName(),
                        "senderRole", senderRole.name(),
                        "applicationId", application.getId().toString().substring(0, 8),
                        "messagePreview", content.length() > 50 ? content.substring(0, 50) + "..." : content
                    )
                );
            }
        }
        
        return saved;
    }

    @Override
    public Page<Message> getMessageHistory(UUID applicationId, Pageable pageable) {
        return messageRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId, pageable);
    }

    @Override
    public void indicateTyping(UUID applicationId, UUID senderId, Role senderRole) {
        Application application = applicationRepository.findById(applicationId)
                .orElse(null);

        if (application == null || (senderRole == Role.CANDIDATE && !application.getCandidateId().equals(senderId))) {
            return;
        }

        java.util.Map<String, Object> typingPayload = java.util.Map.of(
            "senderId", senderId,
            "senderRole", senderRole.name(),
            "timestamp", ZonedDateTime.now().toString()
        );
        
        realtimeEventService.emitApplicationEvent(applicationId, "chat:typing", typingPayload);
    }
}
