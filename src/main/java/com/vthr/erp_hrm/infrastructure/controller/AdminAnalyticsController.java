package com.vthr.erp_hrm.infrastructure.controller;

import com.vthr.erp_hrm.core.model.RecruitmentAnalyticsSummary;
import com.vthr.erp_hrm.core.service.AnalyticsService;
import com.vthr.erp_hrm.infrastructure.controller.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/recruitment")
    public ResponseEntity<ApiResponse<RecruitmentAnalyticsSummary>> recruitmentSummary() {
        RecruitmentAnalyticsSummary data = analyticsService.getRecruitmentSummary();
        return ResponseEntity.ok(ApiResponse.success(data, "OK"));
    }
}
