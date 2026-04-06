package com.vthr.erp_hrm.infrastructure.controller;

import com.vthr.erp_hrm.core.model.ApplicationTaskDocumentType;
import com.vthr.erp_hrm.core.service.ApplicationTaskService;
import com.vthr.erp_hrm.infrastructure.controller.request.CreateApplicationTaskRequest;
import com.vthr.erp_hrm.infrastructure.controller.request.ReviewApplicationTaskRequest;
import com.vthr.erp_hrm.infrastructure.controller.response.ApiResponse;
import com.vthr.erp_hrm.infrastructure.controller.response.ApplicationTaskResponse;
import com.vthr.erp_hrm.infrastructure.security.SecurityRoleResolver;
import com.vthr.erp_hrm.infrastructure.storage.SignedUrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApplicationTaskController {

    private final ApplicationTaskService applicationTaskService;
    private final SignedUrlService signedUrlService;

    @GetMapping("/application-tasks/document-types")
    public ResponseEntity<ApiResponse<List<String>>> documentTypes() {
        List<String> names = Arrays.stream(ApplicationTaskDocumentType.values())
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(names, "OK"));
    }

    @GetMapping("/applications/{applicationId}/tasks")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ApplicationTaskResponse>>> listTasks(
            @PathVariable UUID applicationId,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        var role = SecurityRoleResolver.resolveRole(authentication);
        var tasks = applicationTaskService.listTasks(applicationId, userId, role);
        List<ApplicationTaskResponse> body = tasks.stream()
                .map(t -> ApplicationTaskResponse.fromDomain(t, signedUrlService, false))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(body, "OK"));
    }

    @GetMapping("/applications/{applicationId}/tasks/{taskId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ApplicationTaskResponse>> getTask(
            @PathVariable UUID applicationId,
            @PathVariable UUID taskId,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        var role = SecurityRoleResolver.resolveRole(authentication);
        var task = applicationTaskService.getTask(applicationId, taskId, userId, role);
        return ResponseEntity.ok(ApiResponse.success(
                ApplicationTaskResponse.fromDomain(task, signedUrlService, true),
                "OK"));
    }

    @PostMapping("/applications/{applicationId}/tasks")
    @PreAuthorize("hasAnyRole('HR', 'COMPANY')")
    public ResponseEntity<ApiResponse<ApplicationTaskResponse>> createTask(
            @PathVariable UUID applicationId,
            @Valid @RequestBody CreateApplicationTaskRequest request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        var role = SecurityRoleResolver.resolveRole(authentication);
        var task = applicationTaskService.createTask(
                applicationId,
                userId,
                role,
                request.getTitle(),
                request.getDescription(),
                request.getDocumentType(),
                request.getDueAt());
        return ResponseEntity.ok(ApiResponse.success(
                ApplicationTaskResponse.fromDomain(task, signedUrlService, false),
                "Task created"));
    }

    @PostMapping("/applications/{applicationId}/tasks/{taskId}/attachments")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<ApplicationTaskResponse>> uploadAttachment(
            @PathVariable UUID applicationId,
            @PathVariable UUID taskId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {
        UUID userId = UUID.fromString(authentication.getName());
        var role = SecurityRoleResolver.resolveRole(authentication);
        var task = applicationTaskService.uploadAttachment(applicationId, taskId, userId, role, file);
        return ResponseEntity.ok(ApiResponse.success(
                ApplicationTaskResponse.fromDomain(task, signedUrlService, true),
                "Uploaded"));
    }

    @PatchMapping("/applications/{applicationId}/tasks/{taskId}/review")
    @PreAuthorize("hasAnyRole('HR', 'COMPANY')")
    public ResponseEntity<ApiResponse<ApplicationTaskResponse>> reviewTask(
            @PathVariable UUID applicationId,
            @PathVariable UUID taskId,
            @Valid @RequestBody ReviewApplicationTaskRequest request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        var role = SecurityRoleResolver.resolveRole(authentication);
        var task = applicationTaskService.reviewTask(
                applicationId,
                taskId,
                userId,
                role,
                request.getStatus(),
                request.getHrFeedback());
        return ResponseEntity.ok(ApiResponse.success(
                ApplicationTaskResponse.fromDomain(task, signedUrlService, true),
                "Review saved"));
    }

    @DeleteMapping("/applications/{applicationId}/tasks/{taskId}")
    @PreAuthorize("hasAnyRole('HR', 'COMPANY')")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @PathVariable UUID applicationId,
            @PathVariable UUID taskId,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        var role = SecurityRoleResolver.resolveRole(authentication);
        applicationTaskService.deleteTask(applicationId, taskId, userId, role);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted"));
    }

    @DeleteMapping("/applications/{applicationId}/tasks/{taskId}/attachments/{attachmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(
            @PathVariable UUID applicationId,
            @PathVariable UUID taskId,
            @PathVariable UUID attachmentId,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        var role = SecurityRoleResolver.resolveRole(authentication);
        applicationTaskService.deleteAttachment(applicationId, taskId, attachmentId, userId, role);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted"));
    }
}
