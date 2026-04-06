package com.vthr.erp_hrm.core.service;

import com.vthr.erp_hrm.core.model.Application;
import com.vthr.erp_hrm.core.model.ApplicationStageHistory;
import com.vthr.erp_hrm.core.model.ApplicationStatus;
import com.vthr.erp_hrm.core.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ApplicationService {
    Application applyForJob(UUID jobId, UUID candidateId, String cvUrl);

    Page<Application> getApplicationsByJobId(UUID jobId, Pageable pageable);

    java.util.List<com.vthr.erp_hrm.infrastructure.controller.response.KanbanApplicationResponse> getKanbanApplications(
            UUID jobId);

    Page<Application> getApplicationsByCandidateId(UUID candidateId, Pageable pageable);

    Application updateApplicationStatus(UUID id, ApplicationStatus status, UUID changedBy, String note);

    com.vthr.erp_hrm.infrastructure.controller.response.BulkStatusUpdateResponse bulkRejectApplications(
            java.util.List<UUID> applicationIds, UUID changedBy);

    com.vthr.erp_hrm.infrastructure.controller.response.BulkStatusUpdateResponse bulkUpdateApplicationStatus(
            java.util.List<UUID> applicationIds,
            ApplicationStatus status,
            UUID changedBy,
            String note
    );

    Application getApplicationById(UUID id);

    com.vthr.erp_hrm.infrastructure.controller.response.CandidateApplicationDetailResponse getApplicationDetailForCandidate(
            UUID appId, UUID candidateId);

    /** Ứng viên rút đơn (kiểm tra tin còn OPEN, chưa hết hạn, giai đoạn sớm, chưa có lịch PV). */
    Application withdrawApplicationByCandidate(UUID applicationId, UUID candidateId);

    List<ApplicationStageHistory> getApplicationStageHistory(UUID applicationId, UUID userId, Role role);

    /** HR/ADMIN/COMPANY: xem đủ ngữ cảnh ứng viên + PV + AI (không dùng cho candidate). */
    com.vthr.erp_hrm.infrastructure.controller.response.RecruiterApplicationReviewResponse getApplicationReviewForRecruiter(
            UUID applicationId, UUID userId, Role role);

    /** Ghi chú nội bộ HR trên đơn; không đổi stage. */
    Application updateHrNote(UUID applicationId, String hrNote, UUID actorId, Role role);
}
