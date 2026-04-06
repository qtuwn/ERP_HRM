package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.RecruitmentAnalyticsSummary;
import com.vthr.erp_hrm.core.repository.ApplicationRepository;
import com.vthr.erp_hrm.core.repository.JobRepository;
import com.vthr.erp_hrm.core.repository.UserRepository;
import com.vthr.erp_hrm.core.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    @Override
    public RecruitmentAnalyticsSummary getRecruitmentSummary() {
        return RecruitmentAnalyticsSummary.builder()
                .applicationsByStatus(applicationRepository.countApplicationsGroupedByStatus())
                .jobsByStatus(jobRepository.countJobsGroupedByStatus())
                .usersByRole(userRepository.countUsersGroupedByRole())
                .build();
    }
}
