package com.vthr.erp_hrm.infrastructure.controller;

import com.vthr.erp_hrm.core.model.Application;
import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.service.ApplicationService;
import com.vthr.erp_hrm.core.service.ApplicationAccessService;
import com.vthr.erp_hrm.core.service.ResumeService;
import com.vthr.erp_hrm.infrastructure.controller.request.ApplicationUpdateRequest;
import com.vthr.erp_hrm.infrastructure.controller.request.ApplyWithResumeRequest;
import com.vthr.erp_hrm.infrastructure.controller.request.BulkStatusUpdateRequest;
import com.vthr.erp_hrm.infrastructure.controller.response.ApiResponse;
import com.vthr.erp_hrm.infrastructure.controller.response.ApplicationResponse;
import com.vthr.erp_hrm.infrastructure.controller.response.BulkStatusUpdateResponse;
import com.vthr.erp_hrm.infrastructure.security.SecurityRoleResolver;
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
    private final com.vthr.erp_hrm.core.service.JobService jobService;
    private final FileStorageService fileStorageService;
    private final SignedUrlService signedUrlService;
    private final com.vthr.erp_hrm.core.repository.AIEvaluationRepository aiEvaluationRepository;
    private final ApplicationAccessService applicationAccessService;
    private final ResumeService resumeService;

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

    @PostMapping("/jobs/{jobId}/applications/by-resume")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> applyForJobUsingResume(
            @PathVariable UUID jobId,
            @Valid @RequestBody ApplyWithResumeRequest request,
            Authentication authentication) throws IOException {
        UUID candidateId = UUID.fromString(authentication.getName());
        var resume = resumeService.getMyResume(candidateId, request.getResumeId());
        String cvLogicalPath = fileStorageService.copyResumeToJobCv(resume.getStoragePath(), jobId);
        Application app = applicationService.applyForJob(jobId, candidateId, cvLogicalPath);
        return ResponseEntity.ok(ApiResponse.success(mapAndSignUrl(app), "Applied successfully"));
    }

    @GetMapping("/jobs/{jobId}/applications")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'COMPANY')")
    public ResponseEntity<ApiResponse<Page<ApplicationResponse>>> getApplicationsForJob(
            @PathVariable UUID jobId,
            Pageable pageable,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        Role role = SecurityRoleResolver.resolveRole(authentication);
        applicationAccessService.requireRecruiterForJobTopic(userId, role, jobId);
        Page<ApplicationResponse> apps = applicationService.getApplicationsByJobId(jobId, pageable)
                .map(this::mapAndSignUrl);
        return ResponseEntity.ok(ApiResponse.success(apps, "Fetched applications successfully"));
    }

    @GetMapping("/jobs/{jobId}/applications/kanban")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'COMPANY')")
    public ResponseEntity<ApiResponse<java.util.List<com.vthr.erp_hrm.infrastructure.controller.response.KanbanApplicationResponse>>> getKanbanApplications(
            @PathVariable UUID jobId,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        Role role = SecurityRoleResolver.resolveRole(authentication);
        applicationAccessService.requireRecruiterForJobTopic(userId, role, jobId);
        java.util.List<com.vthr.erp_hrm.infrastructure.controller.response.KanbanApplicationResponse> apps = applicationService
                .getKanbanApplications(jobId);
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
                .map(app -> {
                    ApplicationResponse res = mapAndSignUrl(app);
                    try {
                        res.setJobTitle(jobService.getJobById(app.getJobId()).getTitle());
                    } catch (RuntimeException e) {
                        res.setJobTitle("—");
                    }
                    return res;
                });
        return ResponseEntity.ok(ApiResponse.success(apps, "Fetched your applications successfully"));
    }

    @GetMapping("/users/me/applications/{id}")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<com.vthr.erp_hrm.infrastructure.controller.response.CandidateApplicationDetailResponse>> getMyApplicationDetail(
            @PathVariable UUID id, Authentication authentication) {
        UUID candidateId = UUID.fromString(authentication.getName());
        com.vthr.erp_hrm.infrastructure.controller.response.CandidateApplicationDetailResponse detail = applicationService
                .getApplicationDetailForCandidate(id, candidateId);
        return ResponseEntity.ok(ApiResponse.success(detail, "Fetched application detail successfully"));
    }

    @GetMapping("/users/me/applications/{id}/stage-history")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<java.util.List<com.vthr.erp_hrm.infrastructure.controller.response.ApplicationStageHistoryResponse>>> getMyApplicationStageHistory(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        Role role = SecurityRoleResolver.resolveRole(authentication);
        var list = applicationService.getApplicationStageHistory(id, userId, role).stream()
                .map(com.vthr.erp_hrm.infrastructure.controller.response.ApplicationStageHistoryResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(list, "OK"));
    }

    @GetMapping("/applications/{id}/stage-history")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'COMPANY')")
    public ResponseEntity<ApiResponse<java.util.List<com.vthr.erp_hrm.infrastructure.controller.response.ApplicationStageHistoryResponse>>> getApplicationStageHistoryForRecruiter(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        Role role = SecurityRoleResolver.resolveRole(authentication);
        var list = applicationService.getApplicationStageHistory(id, userId, role).stream()
                .map(com.vthr.erp_hrm.infrastructure.controller.response.ApplicationStageHistoryResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(list, "OK"));
    }

    @PatchMapping("/applications/{id}/status")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'COMPANY')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateApplicationStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ApplicationUpdateRequest request,
            Authentication authentication) {
        UUID changedBy = UUID.fromString(authentication.getName());
        Application updated = applicationService.updateApplicationStatus(id, request.getStatus(), changedBy,
                request.getNote());
        return ResponseEntity.ok(ApiResponse.success(mapAndSignUrl(updated), "Updated status successfully"));
    }

    @PostMapping("/applications/bulk-reject")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'COMPANY')")
    public ResponseEntity<ApiResponse<Void>> bulkRejectApplications(
            @RequestBody com.vthr.erp_hrm.infrastructure.controller.request.BulkRejectRequest request,
            Authentication authentication) {
        UUID hrId = UUID.fromString(authentication.getName());
        applicationService.bulkRejectApplications(request.getApplicationIds(), hrId);
        return ResponseEntity.ok(ApiResponse.success(null, "Bulk rejection processed successfully"));
    }

    @PostMapping("/applications/bulk-status")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'COMPANY')")
    public ResponseEntity<ApiResponse<BulkStatusUpdateResponse>> bulkUpdateStatus(
            @Valid @RequestBody BulkStatusUpdateRequest request,
            Authentication authentication
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        var res = applicationService.bulkUpdateApplicationStatus(
                request.getApplicationIds(),
                request.getStatus(),
                actorId,
                request.getNote()
        );
        return ResponseEntity.ok(ApiResponse.success(res, "Bulk status update processed"));
    }

    @GetMapping("/applications/{id}/ai-evaluation")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'COMPANY')")
    public ResponseEntity<ApiResponse<com.vthr.erp_hrm.core.model.AIEvaluation>> getAiEvaluation(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        Role role = SecurityRoleResolver.resolveRole(authentication);
        applicationAccessService.requireParticipantForMessaging(userId, role, id);
        var eval = aiEvaluationRepository.findByApplicationId(id).orElse(null);
        return ResponseEntity.ok(ApiResponse.success(eval, "Fetched AI evaluation"));
    }
}
