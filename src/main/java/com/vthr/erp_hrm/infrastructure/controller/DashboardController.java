package com.vthr.erp_hrm.infrastructure.controller;

import com.vthr.erp_hrm.core.service.DashboardService;
import com.vthr.erp_hrm.infrastructure.controller.response.ApiResponse;
import com.vthr.erp_hrm.infrastructure.controller.response.DashboardStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    public String getDashboardPage() {
        return "hr/dashboard";
    }

    @GetMapping("/api/dashboard/stats")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    @ResponseBody
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getDashboardStats(),
                "Fetched dashboard analytics successfully"
        ));
    }
}
