package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.JobStatus;
import com.vthr.erp_hrm.core.repository.ApplicationRepository;
import com.vthr.erp_hrm.core.repository.JobRepository;
import com.vthr.erp_hrm.infrastructure.controller.response.DashboardStatsResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @Test
    void getDashboardStats_shouldReturnCorrectAggregations() {
        // Arrange
        when(jobRepository.count()).thenReturn(100L);
        when(jobRepository.countByStatus(JobStatus.OPEN)).thenReturn(40L);
        when(applicationRepository.count()).thenReturn(200L);

        Map<String, Long> grouped = new HashMap<>();
        grouped.put("APPLIED", 100L);
        grouped.put("AI_SCREENING", 0L);
        grouped.put("HR_REVIEW", 30L);
        grouped.put("INTERVIEW", 20L);
        grouped.put("OFFER", 10L);
        grouped.put("HIRED", 10L);
        grouped.put("REJECTED", 30L);
        when(applicationRepository.countApplicationsGroupedByStatus()).thenReturn(grouped);

        // Act
        DashboardStatsResponse response = dashboardService.getDashboardStats();

        // Assert
        assertNotNull(response);
        assertEquals(100L, response.getTotalJobs());
        assertEquals(40L, response.getActiveJobs());
        assertEquals(200L, response.getTotalApplications());
        
        // Assert maps and calculations correctly
        assertEquals(100L, response.getApplicationsByStatus().get("APPLIED"));
        assertEquals(10L, response.getApplicationsByStatus().get("HIRED"));

        // Assert Pass Rate calculation (10 hired out of 200 total = 5.0)
        assertEquals(5.0, response.getPassRate());
    }
}
