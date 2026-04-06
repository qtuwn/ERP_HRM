package com.vthr.erp_hrm.infrastructure.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Payload JSON cho GET /api/jobs khi dùng afterCreatedAt + afterId (keyset).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicJobsKeysetResponse {
    private List<JobResponse> content;
    private boolean hasNext;
    /** ISO-8601; null nếu không còn trang sau */
    private String nextAfterCreatedAt;
    /** null nếu không còn trang sau */
    private String nextAfterId;
}
