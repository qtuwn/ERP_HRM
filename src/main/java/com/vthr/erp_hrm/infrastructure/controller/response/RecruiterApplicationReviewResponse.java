package com.vthr.erp_hrm.infrastructure.controller.response;

import com.vthr.erp_hrm.core.model.AIEvaluation;
import com.vthr.erp_hrm.core.model.Interview;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RecruiterApplicationReviewResponse {
    private ApplicationResponse application;
    private String jobTitle;
    private RecruiterCandidateProfile candidate;
    private List<Interview> interviews;
    private AIEvaluation aiEvaluation;
}
