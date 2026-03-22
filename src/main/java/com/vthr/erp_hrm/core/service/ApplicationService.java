package com.vthr.erp_hrm.core.service;

import com.vthr.erp_hrm.core.model.Application;
import com.vthr.erp_hrm.core.model.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ApplicationService {
    Application applyForJob(UUID jobId, UUID candidateId, String cvUrl);

    Page<Application> getApplicationsByJobId(UUID jobId, Pageable pageable);

    java.util.List<com.vthr.erp_hrm.infrastructure.controller.response.KanbanApplicationResponse> getKanbanApplications(
            UUID jobId);

    Page<Application> getApplicationsByCandidateId(UUID candidateId, Pageable pageable);

    Application updateApplicationStatus(UUID id, ApplicationStatus status, UUID changedBy, String note);

    void bulkRejectApplications(java.util.List<UUID> applicationIds, UUID changedBy);

    Application getApplicationById(UUID id);

    com.vthr.erp_hrm.infrastructure.controller.response.CandidateApplicationDetailResponse getApplicationDetailForCandidate(
            UUID appId, UUID candidateId);
}
