package com.vthr.erp_hrm.infrastructure.controller;

import com.vthr.erp_hrm.infrastructure.controller.request.AdminVerifyCompanyRequest;
import com.vthr.erp_hrm.infrastructure.controller.response.ApiResponse;
import com.vthr.erp_hrm.infrastructure.controller.response.AdminCompanyVerificationResponse;
import com.vthr.erp_hrm.infrastructure.persistence.entity.CompanyEntity;
import com.vthr.erp_hrm.infrastructure.persistence.repository.CompanyRepository;
import com.vthr.erp_hrm.core.service.AuditLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/companies")
@RequiredArgsConstructor
public class AdminCompanyVerificationController {

    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AdminCompanyVerificationResponse>>> listPending() {
        List<CompanyEntity> companies = companyRepository.findByIsVerifiedByAdminFalseOrderByCreatedAtDesc();
        List<AdminCompanyVerificationResponse> data = companies.stream().map(AdminCompanyVerificationResponse::fromEntity).toList();
        return ResponseEntity.ok(ApiResponse.success(data, "Fetched pending companies successfully"));
    }

    @PatchMapping("/{companyId}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminCompanyVerificationResponse> verifyCompany(
            @PathVariable UUID companyId,
            @Valid @RequestBody AdminVerifyCompanyRequest request,
            Authentication authentication
    ) {
        CompanyEntity company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        boolean next = Boolean.TRUE.equals(request.getVerified());
        company.setVerifiedByAdmin(next);
        CompanyEntity saved = companyRepository.save(company);

        UUID actorId = UUID.fromString(authentication.getName());
        auditLogService.logAction(
                actorId,
                next ? "VERIFY_COMPANY" : "UNVERIFY_COMPANY",
                "Company",
                companyId,
                next ? "Approved company verification" : "Rejected company verification"
        );

        return ResponseEntity.ok(AdminCompanyVerificationResponse.fromEntity(saved));
    }
}

