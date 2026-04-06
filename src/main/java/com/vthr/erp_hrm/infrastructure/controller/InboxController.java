package com.vthr.erp_hrm.infrastructure.controller;

import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.service.InboxService;
import com.vthr.erp_hrm.infrastructure.controller.response.ApiResponse;
import com.vthr.erp_hrm.infrastructure.controller.response.RecruiterInboxThreadResponse;
import com.vthr.erp_hrm.infrastructure.security.SecurityRoleResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/inbox")
@RequiredArgsConstructor
public class InboxController {

    private final InboxService inboxService;

    @GetMapping("/recruiter/threads")
    @PreAuthorize("hasAnyRole('HR', 'COMPANY')")
    public ResponseEntity<ApiResponse<Page<RecruiterInboxThreadResponse>>> recruiterThreads(
            @RequestParam(required = false) UUID jobId,
            Pageable pageable,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        Role role = SecurityRoleResolver.resolveRole(authentication);
        Page<RecruiterInboxThreadResponse> page = inboxService.listRecruiterThreads(userId, role, jobId, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Fetched recruiter inbox threads"));
    }
}
