package com.vthr.erp_hrm.core.service;

import com.vthr.erp_hrm.core.model.Role;

import java.util.UUID;

/**
 * Kiểm tra quyền trên đơn ứng tuyển: chat/history vs thao tác recruiter.
 * <p>ADMIN không tham gia pipeline tuyển dụng (Kanban/chat/task/PV); chỉ quản lý tin job ở tầng nền tảng qua {@code JobController}.
 */
public interface ApplicationAccessService {

    /** Ứng viên (owner), hoặc HR/COMPANY cùng công ty với job (đúng rule tạo tin). Không cho ADMIN. */
    void requireParticipantForMessaging(UUID userId, Role role, UUID applicationId);

    /** HR/COMPANY cùng công ty với job — không cho CANDIDATE hay ADMIN. */
    void requireRecruiterForManagement(UUID userId, Role role, UUID applicationId);

    /** Subscribe /topic/jobs/{jobId}: không cho CANDIDATE hay ADMIN; chỉ HR/COMPANY trong phạm vi job. */
    void requireRecruiterForJobTopic(UUID userId, Role role, UUID jobId);
}
