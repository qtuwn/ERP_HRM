package com.vthr.erp_hrm.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecruitmentAnalyticsSummary {
    private Map<String, Long> applicationsByStatus;
    private Map<String, Long> jobsByStatus;
    private Map<String, Long> usersByRole;
}
