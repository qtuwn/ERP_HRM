package com.vthr.erp_hrm.infrastructure.controller.response;

import com.vthr.erp_hrm.core.model.AIEvaluation;
import com.vthr.erp_hrm.core.model.Interview;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CandidateApplicationDetailResponse {
    private ApplicationResponse application;
    private String jobTitle;
    private AIEvaluation aiEvaluation;
    private List<Interview> interviews;
    /** Có thể rút đơn trong điều kiện hiện tại hay không. */
    private WithdrawalEligibilityResponse withdrawalEligibility;
}
