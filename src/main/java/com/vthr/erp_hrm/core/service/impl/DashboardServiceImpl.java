package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.JobStatus;
import com.vthr.erp_hrm.core.model.ApplicationStatus;
import com.vthr.erp_hrm.core.repository.ApplicationRepository;
import com.vthr.erp_hrm.core.repository.JobRepository;
import com.vthr.erp_hrm.core.service.DashboardService;
import com.vthr.erp_hrm.infrastructure.controller.response.DashboardStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;

    @Override
    public DashboardStatsResponse getDashboardStats() {
        long totalJobs = jobRepository.count();
        long activeJobs = jobRepository.countByStatus(JobStatus.OPEN);
        long totalApps = applicationRepository.count();

        List<String> statuses = Arrays.asList("APPLIED", "AI_SCREENING", "HR_REVIEW", "INTERVIEW", "OFFER", "HIRED",
                "REJECTED");
        Map<String, Long> grouped = applicationRepository.countApplicationsGroupedByStatus();
        Map<String, Long> appsByStatus = new HashMap<>();
        long hiredCount = 0;

        for (String status : statuses) {
            long count = grouped.getOrDefault(status, 0L);
            appsByStatus.put(status, count);
            if ("HIRED".equals(status)) {
                hiredCount = count;
            }
        }

        double passRate = (totalApps == 0) ? 0.0 : ((double) hiredCount / totalApps) * 100.0;
        passRate = Math.round(passRate * 10.0) / 10.0; // Round to 1 decimal place

        return DashboardStatsResponse.builder()
                .totalJobs(totalJobs)
                .activeJobs(activeJobs)
                .totalApplications(totalApps)
                .applicationsByStatus(appsByStatus)
                .passRate(passRate)
                .build();
    }
}
