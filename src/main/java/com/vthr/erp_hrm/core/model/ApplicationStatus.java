package com.vthr.erp_hrm.core.model;

public enum ApplicationStatus {
    APPLIED,
    AI_SCREENING,
    HR_REVIEW,
    INTERVIEW,
    OFFER,
    REJECTED,
    HIRED,
    /** Ứng viên chủ động rút đơn (còn trong thời hạn & giai đoạn cho phép). */
    WITHDRAWN
}
