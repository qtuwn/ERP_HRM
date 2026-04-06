package com.vthr.erp_hrm.infrastructure.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalEligibilityResponse {
    private boolean allowed;
    /** Lý do không được rút (null nếu {@code allowed}). */
    private String reason;
}
