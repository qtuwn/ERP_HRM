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
    public ResponseEntity<ApiResponse<Page<JobResponse>>> getOpenJobs(
            @RequestParam(required = false) String q,
            Pageable pageable) {
        Page<JobResponse> jobs = jobService.getOpenJobs(q, pageable).map(JobResponse::fromDomain);
        return ResponseEntity.ok(ApiResponse.success(jobs, "Fetched open jobs successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobResponse>> getPublicJobById(@PathVariable UUID id) {
        JobResponse job = JobResponse.fromDomain(jobService.getPublicJobById(id));
        return ResponseEntity.ok(ApiResponse.success(job, "Fetched job successfully"));
    }

    // HR/Admin APIs
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'COMPANY')")
    public ResponseEntity<ApiResponse<Page<JobResponse>>> getAllJobs(Pageable pageable) {
        Page<JobResponse> jobs = jobService.getAllJobs(pageable).map(JobResponse::fromDomain);
        return ResponseEntity.ok(ApiResponse.success(jobs, "Fetched all jobs successfully"));
    }

    @GetMapping("/department")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'COMPANY')")
    public ResponseEntity<ApiResponse<Page<JobResponse>>> getJobsByDepartment(
            @RequestParam(value = "department", required = false) String department,
            Pageable pageable,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());

        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HR"))) {
            com.vthr.erp_hrm.core.model.User userDetails = userService.getUserById(userId);
            String userDepartment = userDetails.getDepartment();

            if (userDepartment != null && !userDepartment.isBlank()) {
                Page<JobResponse> jobs = jobService.getJobsByDepartment(userDepartment, pageable)
                        .map(JobResponse::fromDomain);
                return ResponseEntity.ok(ApiResponse.success(jobs, "Fetched HR department jobs successfully"));
            }

            if (userDetails.getCompanyId() != null) {
                Page<JobResponse> jobs = jobService.getJobsByCompanyId(userDetails.getCompanyId(), pageable)
                        .map(JobResponse::fromDomain);
                return ResponseEntity.ok(ApiResponse.success(jobs, "Fetched company jobs successfully"));
            }

            return ResponseEntity.ok(ApiResponse.success(Page.empty(pageable), "No jobs found"));
        }

        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_COMPANY"))) {
            com.vthr.erp_hrm.core.model.User userDetails = userService.getUserById(userId);
            if (userDetails.getCompanyId() != null) {
                Page<JobResponse> jobs = jobService.getJobsByCompanyId(userDetails.getCompanyId(), pageable)
                        .map(JobResponse::fromDomain);
                return ResponseEntity.ok(ApiResponse.success(jobs, "Fetched company jobs successfully"));
            }
            return ResponseEntity.ok(ApiResponse.success(Page.empty(pageable), "No jobs found"));
        }

        if (department != null && !department.isEmpty()) {
            Page<JobResponse> jobs = jobService.getJobsByDepartment(department, pageable)
                    .map(JobResponse::fromDomain);
            return ResponseEntity.ok(ApiResponse.success(jobs, "Fetched department jobs successfully"));
        }

        Page<JobResponse> allJobs = jobService.getAllJobs(pageable).map(JobResponse::fromDomain);
        return ResponseEntity.ok(ApiResponse.success(allJobs, "Fetched all jobs successfully"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'COMPANY')")
    public ResponseEntity<ApiResponse<JobResponse>> createJob(
            @Valid @RequestBody JobCreateRequest request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        com.vthr.erp_hrm.core.model.User userDetails = userService.getUserById(userId);

        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HR"))) {
            String userDepartment = userDetails.getDepartment();

            if (userDepartment != null && !userDepartment.isBlank()) {
                if (request.getDepartment() != null && !request.getDepartment().isBlank() &&
                        !userDepartment.equalsIgnoreCase(request.getDepartment())) {
                    throw new RuntimeException("HR can only create jobs for their own department");
                }
                request.setDepartment(userDepartment);
            }
        }

        Job job = Job.builder()
                .title(request.getTitle())
                .industry(request.getIndustry())
                .level(request.getLevel())
                .jobType(request.getJobType())
                .salaryType(request.getSalaryType())
                .salaryMin(request.getSalaryMin())
                .salaryMax(request.getSalaryMax())
                .salaryCurrency(request.getSalaryCurrency())
                .description(request.getDescription())
                .requirements(request.getRequirements())
                .benefits(request.getBenefits())
                .tags(request.getTags())
                .companyName(request.getCompanyName())
                .companyLogo(request.getCompanyLogo())
                .address(request.getAddress())
                .city(request.getCity())
                .companySize(request.getCompanySize())
                .department(request.getDepartment())
                .companyId(userDetails.getCompanyId())
                .requiredSkills(request.getRequiredSkills())
                .notificationEmail(request.getNotificationEmail())
                .numberOfPositions(request.getNumberOfPositions())
                .expiresAt(request.getExpiresAt())
                .build();
        Job created = jobService.createJob(job, userId);
        return ResponseEntity.ok(ApiResponse.success(JobResponse.fromDomain(created), "Created job successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'COMPANY')")
    public ResponseEntity<ApiResponse<JobResponse>> updateJob(
            @PathVariable UUID id,
            @RequestBody JobUpdateRequest request,
            Authentication authentication) {
        Job existing = jobService.getJobById(id);
        validateJobAccess(authentication, existing);

        Job details = Job.builder()
                .title(request.getTitle())
                .industry(request.getIndustry())
                .level(request.getLevel())
                .jobType(request.getJobType())
                .salaryType(request.getSalaryType())
                .salaryMin(request.getSalaryMin())
                .salaryMax(request.getSalaryMax())
                .salaryCurrency(request.getSalaryCurrency())
                .description(request.getDescription())
                .requirements(request.getRequirements())
                .benefits(request.getBenefits())
                .tags(request.getTags())
                .companyName(request.getCompanyName())
                .companyLogo(request.getCompanyLogo())
                .address(request.getAddress())
                .city(request.getCity())
                .companySize(request.getCompanySize())
                .department(request.getDepartment())
                .requiredSkills(request.getRequiredSkills())
                .notificationEmail(request.getNotificationEmail())
                .numberOfPositions(request.getNumberOfPositions())
                .expiresAt(request.getExpiresAt())
                .build();
        Job updated = jobService.updateJob(id, details);
        return ResponseEntity.ok(ApiResponse.success(JobResponse.fromDomain(updated), "Updated job successfully"));
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'COMPANY')")
    public ResponseEntity<ApiResponse<JobResponse>> publishJob(
            @PathVariable UUID id,
            Authentication authentication) {
        Job existing = jobService.getJobById(id);
        validateJobAccess(authentication, existing);
        Job published = jobService.publishJob(id);
        return ResponseEntity.ok(ApiResponse.success(JobResponse.fromDomain(published), "Published job successfully"));
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'COMPANY')")
    public ResponseEntity<ApiResponse<JobResponse>> closeJob(
            @PathVariable UUID id,
            Authentication authentication) {
        Job existing = jobService.getJobById(id);
        validateJobAccess(authentication, existing);
        Job closed = jobService.closeJob(id);
        return ResponseEntity.ok(ApiResponse.success(JobResponse.fromDomain(closed), "Closed job successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'COMPANY')")
    public ResponseEntity<ApiResponse<Void>> deleteJob(
            @PathVariable UUID id,
            Authentication authentication) {
        Job existing = jobService.getJobById(id);
        validateJobAccess(authentication, existing);
        jobService.deleteJob(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted job successfully"));
    }

    private void validateJobAccess(Authentication authentication, Job existingJob) {
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return;
        }

        UUID userId = UUID.fromString(authentication.getName());
        com.vthr.erp_hrm.core.model.User userDetails = userService.getUserById(userId);

        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_COMPANY"))) {
            if (userDetails.getCompanyId() != null &&
                    userDetails.getCompanyId().equals(existingJob.getCompanyId())) {
                return;
            }
            throw new RuntimeException("Access denied: job does not belong to your company");
        }

        String userDepartment = userDetails.getDepartment();
        if (userDepartment != null && !userDepartment.isBlank()) {
            String jobDepartment = existingJob.getDepartment();
            if (jobDepartment != null && userDepartment.equalsIgnoreCase(jobDepartment)) {
                return;
            }
        }

        if (userDetails.getCompanyId() != null &&
                userDetails.getCompanyId().equals(existingJob.getCompanyId())) {
            return;
        }

        throw new RuntimeException("Access denied: you can only manage jobs in your department or company");
    }
}
