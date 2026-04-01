package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.Application;
import com.vthr.erp_hrm.core.model.ApplicationStageHistory;
import com.vthr.erp_hrm.core.model.ApplicationStatus;
import com.vthr.erp_hrm.core.model.Job;
import com.vthr.erp_hrm.core.model.JobStatus;
import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.repository.ApplicationRepository;
import com.vthr.erp_hrm.core.service.ApplicationAccessService;
import com.vthr.erp_hrm.core.service.ApplicationService;
import com.vthr.erp_hrm.core.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import com.vthr.erp_hrm.core.model.User;
import com.vthr.erp_hrm.infrastructure.email.EmailQueueService;
import com.vthr.erp_hrm.infrastructure.controller.response.ApplicationResponse;
import com.vthr.erp_hrm.infrastructure.webhook.WebhookOutboxService;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    @Value("${app.ai.screening.enabled:false}")
    private boolean aiScreeningEnabled;

    private final ApplicationRepository applicationRepository;
    private final JobService jobService;
    private final com.vthr.erp_hrm.infrastructure.ai.AiQueueService aiQueueService;
    private final ApplicationAccessService applicationAccessService;
    private final com.vthr.erp_hrm.core.repository.ApplicationStageHistoryRepository historyRepository;
    private final com.vthr.erp_hrm.core.repository.UserRepository userRepository;
    private final com.vthr.erp_hrm.core.repository.AIEvaluationRepository aiEvaluationRepository;
    private final com.vthr.erp_hrm.core.repository.InterviewRepository interviewRepository;
    private final com.vthr.erp_hrm.infrastructure.websocket.RealtimeEventService realtimeEventService;
    private final EmailQueueService emailQueueService;
    private final com.vthr.erp_hrm.infrastructure.storage.SignedUrlService signedUrlService;
    private final WebhookOutboxService webhookOutboxService;

    @Override
    public Application applyForJob(UUID jobId, UUID candidateId, String cvUrl) {
        Job job = jobService.getJobById(jobId);

        if (job.getStatus() != JobStatus.OPEN) {
            throw new RuntimeException("Cannot apply for a job that is not OPEN");
        }

        if (applicationRepository.existsByJobIdAndCandidateId(jobId, candidateId)) {
            throw new RuntimeException("You have already applied for this job");
        }

        Application application = Application.builder()
                .jobId(jobId)
                .candidateId(candidateId)
                .cvUrl(cvUrl)
                .status(ApplicationStatus.APPLIED) // Fixed to APPLIED from PENDING originally via mapper
                .aiStatus(aiScreeningEnabled ? "AI_QUEUED" : "DISABLED")
                .build();
        Application saved = applicationRepository.save(application);
        if (aiScreeningEnabled) {
            aiQueueService.enqueueApplication(saved.getId());
        }
        realtimeEventService.emitJobEvent(jobId, "application:new", saved);
        webhookOutboxService.enqueueForApplicationApplied(saved.getId());

        User candidate = userRepository.findById(candidateId).orElse(null);
        if (candidate != null) {
            emailQueueService.enqueueEmail(
                    candidate.getEmail(),
                    "Xác nhận Ứng Tuyển: " + job.getTitle(),
                    "apply_confirm",
                    java.util.Map.of(
                            "candidateName", candidate.getFullName(),
                            "jobTitle", job.getTitle(),
                            "applicationId", saved.getId().toString().substring(0, 8)));
        }

        return saved;
    }

    @Override
    public java.util.List<com.vthr.erp_hrm.infrastructure.controller.response.KanbanApplicationResponse> getKanbanApplications(
            UUID jobId) {
        return applicationRepository.findByJobId(jobId).stream().map(app -> {
            com.vthr.erp_hrm.core.model.User candidate = userRepository.findById(app.getCandidateId()).orElse(null);
            String candidateName = candidate != null ? candidate.getFullName() : "Unknown";
            String candidateEmail = candidate != null ? candidate.getEmail() : "Unknown";

            Integer aiScore = null;
            String aiSuitability = null;
            com.vthr.erp_hrm.core.model.AIEvaluation eval = aiEvaluationRepository.findByApplicationId(app.getId())
                    .orElse(null);
            if (eval != null) {
                aiScore = eval.getScore();
                // Worker đang lưu suitability vào discrepancy
                aiSuitability = eval.getDiscrepancy();
            }

            return com.vthr.erp_hrm.infrastructure.controller.response.KanbanApplicationResponse.builder()
                    .id(app.getId())
                    .candidateId(app.getCandidateId())
                    .candidateName(candidateName)
                    .candidateEmail(candidateEmail)
                    .status(app.getStatus() != null ? app.getStatus().name() : null)
                    .aiStatus(app.getAiStatus())
                    .aiScore(aiScore)
                    .aiSuitability(aiSuitability)
                    .cvUrl(app.getCvUrl())
                    .createdAt(app.getCreatedAt())
                    .build();
        })
                .sorted((a, b) -> {
                    int scoreA = a.getAiScore() != null ? a.getAiScore() : -1;
                    int scoreB = b.getAiScore() != null ? b.getAiScore() : -1;
                    return Integer.compare(scoreB, scoreA);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public Page<Application> getApplicationsByJobId(UUID jobId, Pageable pageable) {
        return applicationRepository.findByJobId(jobId, pageable);
    }

    @Override
    public Page<Application> getApplicationsByCandidateId(UUID candidateId, Pageable pageable) {
        return applicationRepository.findByCandidateId(candidateId, pageable);
    }

    @Override
    public Application updateApplicationStatus(UUID id, ApplicationStatus status, UUID changedBy, String note) {
        User changer = userRepository.findById(changedBy)
                .orElseThrow(() -> new RuntimeException("User not found"));
        applicationAccessService.requireRecruiterForManagement(changer.getId(), changer.getRole(), id);

        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        ApplicationStatus oldStatus = application.getStatus();
        if (oldStatus == status)
            return application;

        if (oldStatus == ApplicationStatus.REJECTED || oldStatus == ApplicationStatus.HIRED) {
            throw new RuntimeException("Invalid status transition");
        }

        application.setStatus(status);
        Application saved = applicationRepository.save(application);

        historyRepository.save(com.vthr.erp_hrm.core.model.ApplicationStageHistory.builder()
                .applicationId(saved.getId())
                .fromStage(oldStatus)
                .toStage(status)
                .changedBy(changedBy)
                .note(note)
                .build());

        realtimeEventService.emitJobEvent(saved.getJobId(), "application:stage_changed", saved);
        if (status == ApplicationStatus.REJECTED) {
            webhookOutboxService.enqueueForApplicationRejected(saved.getId());
        }

        Job job = jobService.getJobById(application.getJobId());
        User candidate = userRepository.findById(application.getCandidateId()).orElse(null);
        if (candidate != null) {
            if (status == ApplicationStatus.REJECTED) {
                emailQueueService.enqueueEmail(candidate.getEmail(), "Kết quả Phỏng Vấn: " + job.getTitle(), "rejected",
                        java.util.Map.of(
                                "candidateName", candidate.getFullName(), "jobTitle", job.getTitle()));
            } else if (status == ApplicationStatus.OFFER || status == ApplicationStatus.HIRED) {
                emailQueueService.enqueueEmail(candidate.getEmail(), "Thư Mời Nhận Việc: " + job.getTitle(), "hired",
                        java.util.Map.of(
                                "candidateName", candidate.getFullName(), "jobTitle", job.getTitle()));
            }
        }

        return saved;
    }

    @Override
    public Application getApplicationById(UUID id) {
        return applicationRepository.findById(id).orElseThrow(() -> new RuntimeException("Application not found"));
    }

    @Override
    public void bulkRejectApplications(java.util.List<UUID> applicationIds, UUID changedBy) {
        if (applicationIds == null || applicationIds.isEmpty())
            return;
        for (UUID id : applicationIds) {
            try {
                updateApplicationStatus(id, ApplicationStatus.REJECTED, changedBy, "Bulk Rejected from Kanban");
            } catch (Exception e) {
                // Skip if error, but typically works. Could log here.
            }
        }
    }

    @Override
    public com.vthr.erp_hrm.infrastructure.controller.response.BulkStatusUpdateResponse bulkUpdateApplicationStatus(
            java.util.List<UUID> applicationIds,
            ApplicationStatus status,
            UUID changedBy,
            String note
    ) {
        if (applicationIds == null || applicationIds.isEmpty()) {
            return com.vthr.erp_hrm.infrastructure.controller.response.BulkStatusUpdateResponse.builder()
                    .succeededIds(java.util.List.of())
                    .failed(java.util.Map.of())
                    .build();
        }
        java.util.List<UUID> ok = new java.util.ArrayList<>();
        java.util.Map<UUID, String> failed = new java.util.LinkedHashMap<>();

        for (UUID id : applicationIds) {
            try {
                updateApplicationStatus(id, status, changedBy, note);
                ok.add(id);
            } catch (RuntimeException ex) {
                failed.put(id, ex.getMessage() != null ? ex.getMessage() : "Failed");
            }
        }

        return com.vthr.erp_hrm.infrastructure.controller.response.BulkStatusUpdateResponse.builder()
                .succeededIds(ok)
                .failed(failed)
                .build();
    }

    @Override
    public com.vthr.erp_hrm.infrastructure.controller.response.CandidateApplicationDetailResponse getApplicationDetailForCandidate(
            UUID appId, UUID candidateId) {
        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        // Access control: verify candidate owns the application
        if (!app.getCandidateId().equals(candidateId)) {
            throw new RuntimeException("Access Denied");
        }

        // Build response with application data, job title, and related entities
        ApplicationResponse appResponse = ApplicationResponse.fromDomain(app);
        if (appResponse.getCvUrl() != null) {
            appResponse.setCvUrl(signedUrlService.generateSignedUrl("/api/files/cvs", app.getCvUrl()));
        }

        Job job = jobService.getJobById(app.getJobId());
        String jobTitle = job.getTitle();
        appResponse.setJobTitle(jobTitle);

        com.vthr.erp_hrm.core.model.AIEvaluation aiEval = aiEvaluationRepository.findByApplicationId(appId)
                .orElse(null);
        java.util.List<com.vthr.erp_hrm.core.model.Interview> interviews = interviewRepository
                .findByApplicationId(appId);

        return com.vthr.erp_hrm.infrastructure.controller.response.CandidateApplicationDetailResponse
                .builder()
                .application(appResponse)
                .jobTitle(jobTitle)
                .aiEvaluation(aiEval)
                .interviews(interviews)
                .build();
    }

    @Override
    public List<ApplicationStageHistory> getApplicationStageHistory(UUID applicationId, UUID userId, Role role) {
        applicationAccessService.requireParticipantForMessaging(userId, role, applicationId);
        return historyRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId);
    }
}
