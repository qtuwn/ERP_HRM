package com.vthr.erp_hrm.core.model;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/** Một trang danh sách việc làm OPEN theo keyset (createdAt desc, id desc), không dùng OFFSET. */
public record OpenJobsKeysetSlice(
        List<Job> jobs,
        boolean hasNext,
        ZonedDateTime nextAfterCreatedAt,
        UUID nextAfterId) {
}
