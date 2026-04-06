package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.RecruitmentAnalyticsSummary;
import com.vthr.erp_hrm.core.repository.ApplicationRepository;
import com.vthr.erp_hrm.core.repository.JobRepository;
import com.vthr.erp_hrm.core.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    @Test
    void getRecruitmentSummary_aggregatesRepositories() {
        when(applicationRepository.countApplicationsGroupedByStatus())
                .thenReturn(Map.of("APPLIED", 3L, "HIRED", 1L));
        when(jobRepository.countJobsGroupedByStatus()).thenReturn(Map.of("OPEN", 5L));
        when(userRepository.countUsersGroupedByRole()).thenReturn(Map.of("CANDIDATE", 10L));

        RecruitmentAnalyticsSummary s = analyticsService.getRecruitmentSummary();

        assertEquals(3L, s.getApplicationsByStatus().get("APPLIED"));
        assertEquals(5L, s.getJobsByStatus().get("OPEN"));
        assertEquals(10L, s.getUsersByRole().get("CANDIDATE"));
    }
}
