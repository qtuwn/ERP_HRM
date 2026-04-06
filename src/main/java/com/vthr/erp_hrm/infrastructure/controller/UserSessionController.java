package com.vthr.erp_hrm.infrastructure.controller;

import com.vthr.erp_hrm.core.model.UserSessionItem;
import com.vthr.erp_hrm.core.service.UserSessionService;
import com.vthr.erp_hrm.infrastructure.controller.request.IdentifySessionRequest;
import com.vthr.erp_hrm.infrastructure.controller.response.ApiResponse;
import com.vthr.erp_hrm.infrastructure.controller.response.IdentifySessionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/me/sessions")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserSessionController {

    private final UserSessionService userSessionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserSessionItem>>> list(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(userSessionService.listSessions(userId), "OK"));
    }

    /**
     * Gửi refresh token hiện tại (body) để server trả về id phiên tương ứng — dùng đánh dấu “thiết bị này”.
     */
    @PostMapping("/identify")
    public ResponseEntity<ApiResponse<IdentifySessionResponse>> identify(
            Authentication authentication,
            @Valid @RequestBody IdentifySessionRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        UUID sessionId = userSessionService.findSessionIdForRefreshToken(userId, request.getRefreshToken())
                .orElse(null);
        IdentifySessionResponse body = IdentifySessionResponse.builder().sessionId(sessionId).build();
        return ResponseEntity.ok(ApiResponse.success(body, "OK"));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> revoke(
            Authentication authentication,
            @PathVariable UUID sessionId) {
        UUID userId = UUID.fromString(authentication.getName());
        if (!userSessionService.revokeSession(userId, sessionId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Session revoked"));
    }

    @PostMapping("/revoke-all")
    public ResponseEntity<ApiResponse<Void>> revokeAll(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        userSessionService.revokeAllSessions(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "All sessions revoked"));
    }
}
