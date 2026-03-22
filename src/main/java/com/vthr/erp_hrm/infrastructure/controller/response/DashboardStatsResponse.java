package com.vthr.erp_hrm.infrastructure.controller.response;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class DashboardStatsResponse {
    private long totalJobs;
    private long activeJobs;
    private long totalApplications;
    private Map<String, Long> applicationsByStatus;
    private double passRate;
}
