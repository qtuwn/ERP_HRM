package com.vthr.erp_hrm.infrastructure.controller;

import com.vthr.erp_hrm.core.model.Job;
import com.vthr.erp_hrm.core.service.JobService;
import com.vthr.erp_hrm.infrastructure.controller.request.JobCreateRequest;
import com.vthr.erp_hrm.infrastructure.controller.request.JobUpdateRequest;
import com.vthr.erp_hrm.infrastructure.controller.response.ApiResponse;
import com.vthr.erp_hrm.infrastructure.controller.response.JobResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    // Public APIs
    @GetMapping
    public ResponseEntity<ApiResponse<Page<JobResponse>>> getOpenJobs(Pageable pageable) {
        Page<JobResponse> jobs = jobService.getOpenJobs(pageable).map(JobResponse::fromDomain);
        return ResponseEntity.ok(ApiResponse.success(jobs, "Fetched open jobs successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobResponse>> getPublicJobById(@PathVariable UUID id) {
        JobResponse job = JobResponse.fromDomain(jobService.getPublicJobById(id));
        return ResponseEntity.ok(ApiResponse.success(job, "Fetched job successfully"));
    }

    // HR/Admin APIs
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<JobResponse>>> getAllJobs(Pageable pageable) {
        Page<JobResponse> jobs = jobService.getAllJobs(pageable).map(JobResponse::fromDomain);
        return ResponseEntity.ok(ApiResponse.success(jobs, "Fetched all jobs successfully"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<JobResponse>> createJob(@Valid @RequestBody JobCreateRequest request, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        Job job = Job.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .department(request.getDepartment())
                .requiredSkills(request.getRequiredSkills())
                .expiresAt(request.getExpiresAt())
                .build();
        Job created = jobService.createJob(job, userId);
        return ResponseEntity.ok(ApiResponse.success(JobResponse.fromDomain(created), "Created job successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<JobResponse>> updateJob(@PathVariable UUID id, @RequestBody JobUpdateRequest request) {
        Job details = Job.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .department(request.getDepartment())
                .requiredSkills(request.getRequiredSkills())
                .expiresAt(request.getExpiresAt())
                .build();
        Job updated = jobService.updateJob(id, details);
        return ResponseEntity.ok(ApiResponse.success(JobResponse.fromDomain(updated), "Updated job successfully"));
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<JobResponse>> publishJob(@PathVariable UUID id) {
        Job published = jobService.publishJob(id);
        return ResponseEntity.ok(ApiResponse.success(JobResponse.fromDomain(published), "Published job successfully"));
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<JobResponse>> closeJob(@PathVariable UUID id) {
        Job closed = jobService.closeJob(id);
        return ResponseEntity.ok(ApiResponse.success(JobResponse.fromDomain(closed), "Closed job successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteJob(@PathVariable UUID id) {
        jobService.deleteJob(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted job successfully"));
    }
}
