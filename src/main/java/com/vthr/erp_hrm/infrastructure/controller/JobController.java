package com.vthr.erp_hrm.infrastructure.controller;

import com.vthr.erp_hrm.core.model.Job;
import com.vthr.erp_hrm.core.service.JobService;
import com.vthr.erp_hrm.core.service.UserService;
import com.vthr.erp_hrm.infrastructure.controller.request.JobCreateRequest;
import com.vthr.erp_hrm.infrastructure.controller.request.JobUpdateRequest;
import com.vthr.erp_hrm.infrastructure.controller.response.ApiResponse;
import com.vthr.erp_hrm.infrastructure.controller.response.JobFilterOptionsResponse;
import com.vthr.erp_hrm.infrastructure.controller.response.JobResponse;
import com.vthr.erp_hrm.infrastructure.controller.response.PublicJobsKeysetResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String skill,
            Pageable pageable) {
        Page<JobResponse> jobs = jobService
                .getOpenJobs(q, city, industry, jobType, level, skill, pageable)
                .map(JobResponse::fromDomain);
        return ResponseEntity.ok(ApiResponse.success(jobs, "Fetched open jobs successfully"));
    }

    /**
     * Feed việc làm OPEN: phân trang keyset (afterCreatedAt + afterId của tin cuối trang trước).
     * Trang đầu: không gửi after*. Dùng cho tải thêm / cuộn vô hạn, tránh OFFSET lớn.
     */
    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<PublicJobsKeysetResponse>> getOpenJobsFeedKeyset(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime afterCreatedAt,
            @RequestParam(required = false) UUID afterId,
            @RequestParam(defaultValue = "10") int size) {
        if ((afterCreatedAt == null) != (afterId == null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "afterCreatedAt và afterId phải cùng có hoặc cùng không");
        }
        int n = Math.min(100, Math.max(1, size));
        var slice = jobService.getOpenJobsKeyset(q, city, industry, jobType, level, skill, afterCreatedAt, afterId, n);
        PublicJobsKeysetResponse body = PublicJobsKeysetResponse.builder()
                .content(slice.jobs().stream().map(JobResponse::fromDomain).toList())
                .hasNext(slice.hasNext())
                .nextAfterCreatedAt(slice.nextAfterCreatedAt() != null ? slice.nextAfterCreatedAt().toString() : null)
                .nextAfterId(slice.nextAfterId() != null ? slice.nextAfterId().toString() : null)
                .build();
        return ResponseEntity.ok(ApiResponse.success(body, "Fetched open jobs (keyset)"));
    }

    @GetMapping("/filter-options")
    public ResponseEntity<ApiResponse<JobFilterOptionsResponse>> getOpenJobFilterOptions() {
        var o = jobService.getOpenJobFilterOptions();
        JobFilterOptionsResponse body = JobFilterOptionsResponse.builder()
                .cities(o.cities())
                .industries(o.industries())
                .jobTypes(o.jobTypes())
                .levels(o.levels())
                .build();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                .body(ApiResponse.success(body, "Fetched job filter options"));
    }

    /** Công ty có ít nhất một tin OPEN — dùng cho menu Công ty (career). Không dùng /{id} để tránh xung đột route. */
    @GetMapping("/meta/career-companies")
    public ResponseEntity<ApiResponse<List<String>>> getCareerCompanyNames() {
        List<String> names = jobService.getCareerCompanyNames();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                .body(ApiResponse.success(names, "Fetched career company names"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobResponse>> getPublicJobById(@PathVariable UUID id) {
        JobResponse job = JobResponse.fromDomain(jobService.getPublicJobById(id));
        return ResponseEntity.ok(ApiResponse.success(job, "Fetched job successfully"));
    }

    // HR/Admin APIs
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
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
            if (userDetails.getCompanyId() == null) {
                return ResponseEntity.ok(ApiResponse.success(Page.empty(pageable), "No jobs found"));
            }
            Page<JobResponse> jobs = jobService
                    .getJobsByCompanyIdAndCreatedBy(userDetails.getCompanyId(), userId, pageable)
                    .map(JobResponse::fromDomain);
            return ResponseEntity.ok(ApiResponse.success(jobs, "Fetched HR own jobs successfully"));
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
    @PreAuthorize("hasAnyRole('HR', 'COMPANY')")
    public ResponseEntity<ApiResponse<JobResponse>> createJob(
            @Valid @RequestBody JobCreateRequest request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        com.vthr.erp_hrm.core.model.User userDetails = userService.getUserById(userId);

        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HR"))) {
            if (userDetails.getCompanyId() == null) {
                throw new RuntimeException("HR account is missing companyId");
            }
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
    @PreAuthorize("hasAnyRole('HR', 'COMPANY')")
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

        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HR"))) {
            if (userDetails.getCompanyId() == null
                    || existingJob.getCompanyId() == null
                    || !userDetails.getCompanyId().equals(existingJob.getCompanyId())) {
                throw new RuntimeException("Access denied: job does not belong to your company");
            }
            if (existingJob.getCreatedBy() == null || !existingJob.getCreatedBy().equals(userId)) {
                throw new RuntimeException("Access denied: you can only manage jobs you created");
            }
            return;
        }

        throw new RuntimeException("Access denied");
    }
}
