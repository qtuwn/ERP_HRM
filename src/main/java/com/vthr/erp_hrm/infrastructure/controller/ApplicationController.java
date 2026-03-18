package com.vthr.erp_hrm.infrastructure.controller;

import com.vthr.erp_hrm.core.model.Application;
import com.vthr.erp_hrm.core.service.ApplicationService;
import com.vthr.erp_hrm.infrastructure.controller.request.ApplicationUpdateRequest;
import com.vthr.erp_hrm.infrastructure.controller.response.ApiResponse;
import com.vthr.erp_hrm.infrastructure.controller.response.ApplicationResponse;
import com.vthr.erp_hrm.infrastructure.storage.FileStorageService;
import com.vthr.erp_hrm.infrastructure.storage.SignedUrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final FileStorageService fileStorageService;
    private final SignedUrlService signedUrlService;

    private ApplicationResponse mapAndSignUrl(Application app) {
        ApplicationResponse res = ApplicationResponse.fromDomain(app);
        if (res.getCvUrl() != null) {
            res.setCvUrl(signedUrlService.generateSignedUrl("/api/files/cvs", app.getCvUrl()));
        }
        return res;
    }

    @PostMapping("/jobs/{jobId}/applications")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> applyForJob(
            @PathVariable UUID jobId,
            @RequestParam("cv") MultipartFile cv,
            Authentication authentication) throws IOException {
        
        UUID candidateId = UUID.fromString(authentication.getName());
        String cvLogicalPath = fileStorageService.storeFile(cv, jobId);
        
        Application app = applicationService.applyForJob(jobId, candidateId, cvLogicalPath);
        return ResponseEntity.ok(ApiResponse.success(mapAndSignUrl(app), "Applied successfully"));
    }

    @GetMapping("/jobs/{jobId}/applications")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<ApplicationResponse>>> getApplicationsForJob(
            @PathVariable UUID jobId, Pageable pageable) {
        Page<ApplicationResponse> apps = applicationService.getApplicationsByJobId(jobId, pageable)
                .map(this::mapAndSignUrl);
        return ResponseEntity.ok(ApiResponse.success(apps, "Fetched applications successfully"));
    }

    @GetMapping("/jobs/{jobId}/applications/kanban")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<java.util.List<com.vthr.erp_hrm.infrastructure.controller.response.KanbanApplicationResponse>>> getKanbanApplications(
            @PathVariable UUID jobId) {
        java.util.List<com.vthr.erp_hrm.infrastructure.controller.response.KanbanApplicationResponse> apps = applicationService.getKanbanApplications(jobId);
        apps.forEach(res -> {
            if (res.getCvUrl() != null) {
                res.setCvUrl(signedUrlService.generateSignedUrl("/api/files/cvs", res.getCvUrl()));
            }
        });
        return ResponseEntity.ok(ApiResponse.success(apps, "Fetched Kanban applications successfully"));
    }

    @GetMapping("/users/me/applications")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<Page<ApplicationResponse>>> getMyApplications(
            Authentication authentication, Pageable pageable) {
        UUID candidateId = UUID.fromString(authentication.getName());
        Page<ApplicationResponse> apps = applicationService.getApplicationsByCandidateId(candidateId, pageable)
                .map(this::mapAndSignUrl);
        return ResponseEntity.ok(ApiResponse.success(apps, "Fetched your applications successfully"));
    }

    @PatchMapping("/applications/{id}/status")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateApplicationStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ApplicationUpdateRequest request,
            Authentication authentication) {
        UUID changedBy = UUID.fromString(authentication.getName());
        Application updated = applicationService.updateApplicationStatus(id, request.getStatus(), changedBy, request.getNote());
        return ResponseEntity.ok(ApiResponse.success(mapAndSignUrl(updated), "Updated status successfully"));
    }
}
