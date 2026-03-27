package com.vthr.erp_hrm.infrastructure.controller;

import com.vthr.erp_hrm.core.model.Job;
import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.service.JobService;
import com.vthr.erp_hrm.core.service.UserService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final UserService userService;

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

    @GetMapping("/department")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<JobResponse>>> getJobsByDepartment(
            @RequestParam(value = "department", required = false) String department,
            Pageable pageable,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());

        // If HR, restrict to their department only
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HR"))) {
            com.vthr.erp_hrm.core.model.User userDetails = userService.getUserById(userId);
            String userDepartment = userDetails.getDepartment();
            if (userDepartment == null || userDepartment.isBlank()) {
                throw new RuntimeException("HR account is not assigned to any department");
            }
            Page<JobResponse> jobs = jobService.getJobsByDepartment(userDepartment, pageable)
                    .map(JobResponse::fromDomain);
            return ResponseEntity.ok(ApiResponse.success(jobs, "Fetched HR department jobs successfully"));
        }

        // If ADMIN, can optionally filter by specific department or get all
        if (department != null && !department.isEmpty()) {
            Page<JobResponse> jobs = jobService.getJobsByDepartment(department, pageable)
                    .map(JobResponse::fromDomain);
            return ResponseEntity.ok(ApiResponse.success(jobs, "Fetched department jobs successfully"));
        }

        // Admin without department filter - return all
        Page<JobResponse> allJobs = jobService.getAllJobs(pageable).map(JobResponse::fromDomain);
        return ResponseEntity.ok(ApiResponse.success(allJobs, "Fetched all jobs successfully"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<JobResponse>> createJob(
            @Valid @RequestBody JobCreateRequest request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());

        // If HR, enforce department match
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HR"))) {
            com.vthr.erp_hrm.core.model.User userDetails = userService.getUserById(userId);
            String userDepartment = userDetails.getDepartment();
            if (userDepartment == null || userDepartment.isBlank()) {
                throw new RuntimeException("HR account is not assigned to any department");
            }

            if (request.getDepartment() != null && !request.getDepartment().isBlank() &&
                    !userDepartment.equalsIgnoreCase(request.getDepartment())) {
                throw new RuntimeException("HR can only create jobs for their own department");
            }

            // Always enforce HR department from profile to avoid invalid payloads.
            request.setDepartment(userDepartment);
        }

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
    public ResponseEntity<ApiResponse<JobResponse>> updateJob(
            @PathVariable UUID id,
            @RequestBody JobUpdateRequest request,
            Authentication authentication) {
        Job existing = jobService.getJobById(id);
        validateJobAccess(authentication, existing.getDepartment());

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
    public ResponseEntity<ApiResponse<JobResponse>> publishJob(
            @PathVariable UUID id,
            Authentication authentication) {
        Job existing = jobService.getJobById(id);
        validateJobAccess(authentication, existing.getDepartment());
        Job published = jobService.publishJob(id);
        return ResponseEntity.ok(ApiResponse.success(JobResponse.fromDomain(published), "Published job successfully"));
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<JobResponse>> closeJob(
            @PathVariable UUID id,
            Authentication authentication) {
        Job existing = jobService.getJobById(id);
        validateJobAccess(authentication, existing.getDepartment());
        Job closed = jobService.closeJob(id);
        return ResponseEntity.ok(ApiResponse.success(JobResponse.fromDomain(closed), "Closed job successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteJob(
            @PathVariable UUID id,
            Authentication authentication) {
        Job existing = jobService.getJobById(id);
        validateJobAccess(authentication, existing.getDepartment());
        jobService.deleteJob(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted job successfully"));
    }

    private void validateJobAccess(Authentication authentication, String jobDepartment) {
        // ADMIN can access all jobs
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return;
        }

        // HR can only access jobs of their own department
        UUID userId = UUID.fromString(authentication.getName());
        com.vthr.erp_hrm.core.model.User userDetails = userService.getUserById(userId);
        String userDepartment = userDetails.getDepartment();

        if (userDepartment == null || userDepartment.isBlank()) {
            throw new RuntimeException("HR account is not assigned to any department");
        }

        if (jobDepartment == null || !userDepartment.equalsIgnoreCase(jobDepartment)) {
            throw new RuntimeException("Access denied: you can only manage jobs in your department");
        }
    }
}
