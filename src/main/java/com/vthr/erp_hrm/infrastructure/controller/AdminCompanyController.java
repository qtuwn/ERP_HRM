package com.vthr.erp_hrm.infrastructure.controller;

import com.vthr.erp_hrm.infrastructure.controller.response.ApiResponse;
import com.vthr.erp_hrm.infrastructure.controller.response.CompanyOptionResponse;
import com.vthr.erp_hrm.infrastructure.persistence.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/companies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCompanyController {

    private final CompanyRepository companyRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CompanyOptionResponse>>> getCompanies() {
        List<CompanyOptionResponse> data = companyRepository.findAll().stream()
                .map(c -> CompanyOptionResponse.builder().id(c.getId()).name(c.getName()).build())
                .toList();
        return ResponseEntity.ok(ApiResponse.success(data, "Fetched companies successfully"));
    }
}

