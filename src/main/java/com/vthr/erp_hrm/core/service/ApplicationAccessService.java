package com.vthr.erp_hrm.core.service;

import com.vthr.erp_hrm.core.model.Role;

import java.util.UUID;

/**
 * Kiểm tra quyền trên đơn ứng tuyển: chat/history vs thao tác recruiter.
 */
public interface ApplicationAccessService {

    /** Ứng viên (owner), hoặc HR/COMPANY cùng công ty với job, hoặc ADMIN. */
    void requireParticipantForMessaging(UUID userId, Role role, UUID applicationId);

    /** Chỉ ADMIN hoặc HR/COMPANY cùng công ty với job — không cho CANDIDATE. */
    void requireRecruiterForManagement(UUID userId, Role role, UUID applicationId);

    /** Subscribe /topic/jobs/{jobId}: không cho CANDIDATE; recruiter cùng company hoặc ADMIN. */
    void requireRecruiterForJobTopic(UUID userId, Role role, UUID jobId);
}
