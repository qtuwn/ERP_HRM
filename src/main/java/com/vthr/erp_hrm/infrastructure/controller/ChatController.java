package com.vthr.erp_hrm.infrastructure.controller;

import com.vthr.erp_hrm.core.model.Message;
import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.service.ChatService;
import com.vthr.erp_hrm.infrastructure.controller.request.MessageRequest;
import com.vthr.erp_hrm.infrastructure.controller.response.ApiResponse;
import com.vthr.erp_hrm.infrastructure.controller.response.MessageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/applications/{applicationId}/messages")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<MessageResponse>>> getMessageHistory(
            @PathVariable UUID applicationId,
            @PageableDefault(size = 50) Pageable pageable) {

        Page<Message> msgPage = chatService.getMessageHistory(applicationId, pageable);
        Page<MessageResponse> responsePage = msgPage.map(this::mapToResponse);

        return ResponseEntity.ok(ApiResponse.success(responsePage, "History fetched"));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable UUID applicationId,
            @Valid @RequestBody MessageRequest request,
            Authentication authentication) {

        UUID senderId = UUID.fromString(authentication.getName());
        Role senderRole = authentication.getAuthorities().stream()
                .findFirst()
                .map(auth -> Role.valueOf(auth.getAuthority().replace("ROLE_", "")))
                .orElse(Role.CANDIDATE);

        Message saved = chatService.sendMessage(applicationId, senderId, senderRole, request.getContent());
        
        return ResponseEntity.ok(ApiResponse.success(mapToResponse(saved), "Message sent"));
    }

    private MessageResponse mapToResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .applicationId(message.getApplicationId())
                .senderId(message.getSenderId())
                .senderRole(message.getSenderRole() != null ? message.getSenderRole().name() : null)
                .content(message.getContent())
                .readAt(message.getReadAt())
                .createdAt(message.getCreatedAt())
                .build();
    }

    @PostMapping("/typing")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> indicateTyping(
            @PathVariable UUID applicationId,
            Authentication authentication) {

        UUID senderId = UUID.fromString(authentication.getName());
        Role senderRole = authentication.getAuthorities().stream()
                .findFirst()
                .map(auth -> Role.valueOf(auth.getAuthority().replace("ROLE_", "")))
                .orElse(Role.CANDIDATE);

        chatService.indicateTyping(applicationId, senderId, senderRole);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Typing indicator sent"));
    }
}
