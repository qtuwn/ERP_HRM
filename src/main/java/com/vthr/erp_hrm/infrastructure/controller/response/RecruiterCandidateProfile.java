package com.vthr.erp_hrm.infrastructure.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Thông tin ứng viên cho màn review HR (không chứa dữ liệu nhạy cảm như password). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecruiterCandidateProfile {
    private UUID id;
    private String fullName;
    private String email;
    private String phone;
}
