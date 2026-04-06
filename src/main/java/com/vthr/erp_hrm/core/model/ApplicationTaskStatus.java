package com.vthr.erp_hrm.core.model;

public enum ApplicationTaskStatus {
    /** HR vừa giao, ứng viên chưa nộp / chưa đủ tài liệu. */
    OPEN,
    /** Ứng viên đã tải lên, chờ HR xem xét. */
    SUBMITTED,
    /** HR chấp nhận. */
    APPROVED,
    /** HR từ chối — ứng viên có thể nộp lại. */
    REJECTED
}
