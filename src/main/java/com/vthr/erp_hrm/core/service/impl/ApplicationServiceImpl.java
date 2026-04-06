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
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.vthr.erp_hrm.core.model.AIEvaluation;
import com.vthr.erp_hrm.core.model.User;
import com.vthr.erp_hrm.infrastructure.email.EmailQueueService;
import com.vthr.erp_hrm.infrastructure.controller.response.ApplicationResponse;
import com.vthr.erp_hrm.infrastructure.controller.response.WithdrawalEligibilityResponse;
import com.vthr.erp_hrm.infrastructure.webhook.WebhookOutboxService;
import com.vthr.erp_hrm.core.model.NotificationType;
import com.vthr.erp_hrm.core.service.NotificationService;

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
    private final NotificationService notificationService;
    private final Clock clock;

    private static final Set<ApplicationStatus> WITHDRAWABLE_STATUSES =
            EnumSet.of(ApplicationStatus.APPLIED, ApplicationStatus.AI_SCREENING, ApplicationStatus.HR_REVIEW);

    @Override
    public Application applyForJob(UUID jobId, UUID candidateId, String cvUrl) {
        Job job = jobService.getJobById(jobId);

        if (job.getStatus() != JobStatus.OPEN) {
            throw new RuntimeException("Cannot apply for a job that is not OPEN");
        }

        var existingPair = applicationRepository.findByJobIdAndCandidateId(jobId, candidateId);
        if (existingPair.isPresent()) {
            if (existingPair.get().getStatus() == ApplicationStatus.WITHDRAWN) {
                applicationRepository.deleteById(existingPair.get().getId());
            } else {
                throw new RuntimeException("You have already applied for this job");
            }
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
        List<Application> apps = applicationRepository.findByJobId(jobId).stream()
                .filter(app -> app.getStatus() != ApplicationStatus.WITHDRAWN)
                .collect(Collectors.toList());
        if (apps.isEmpty()) {
            return List.of();
        }

        List<UUID> candidateIds = apps.stream().map(Application::getCandidateId).distinct().collect(Collectors.toList());
        List<UUID> applicationIds = apps.stream().map(Application::getId).collect(Collectors.toList());

        Map<UUID, User> userById = userRepository.findAllById(candidateIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        Map<UUID, AIEvaluation> evalByAppId = aiEvaluationRepository.findAllByApplicationIdIn(applicationIds).stream()
                .collect(Collectors.toMap(AIEvaluation::getApplicationId, Function.identity(), (a, b) -> a));

        return apps.stream()
                .map(app -> {
                    User candidate = userById.get(app.getCandidateId());
                    String candidateName = candidate != null ? candidate.getFullName() : "Unknown";
                    String candidateEmail = candidate != null ? candidate.getEmail() : "Unknown";

                    Integer aiScore = null;
                    String aiSuitability = null;
                    AIEvaluation eval = evalByAppId.get(app.getId());
                    if (eval != null) {
                        aiScore = eval.getScore();
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
                .collect(Collectors.toList());
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

        if (oldStatus == ApplicationStatus.WITHDRAWN) {
            throw new RuntimeException("Application was withdrawn by candidate");
        }

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
        // In-app notification cho ứng viên (không phụ thuộc email).
        try {
            notificationService.create(
                    saved.getCandidateId(),
                    NotificationType.APPLICATION_STAGE_CHANGED,
                    "Hồ sơ ứng tuyển đã được cập nhật",
                    "Giai đoạn mới: " + status.name().replace('_', ' '),
                    "/candidate/applications?applicationId=" + saved.getId(),
                    null
            );
        } catch (Exception ignored) {
            // Không làm fail luồng chính nếu notification gặp lỗi.
        }
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
    public com.vthr.erp_hrm.infrastructure.controller.response.BulkStatusUpdateResponse bulkRejectApplications(
            java.util.List<UUID> applicationIds, UUID changedBy) {
        return bulkUpdateApplicationStatus(
                applicationIds, ApplicationStatus.REJECTED, changedBy, "Bulk Rejected from Kanban");
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

        WithdrawalEligibilityResponse withdrawalEligibility = evaluateWithdrawalEligibility(appId, app, job);

        return com.vthr.erp_hrm.infrastructure.controller.response.CandidateApplicationDetailResponse
                .builder()
                .application(appResponse)
                .jobTitle(jobTitle)
                .aiEvaluation(aiEval)
                .interviews(interviews)
                .withdrawalEligibility(withdrawalEligibility)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public com.vthr.erp_hrm.infrastructure.controller.response.RecruiterApplicationReviewResponse getApplicationReviewForRecruiter(
            UUID appId, UUID userId, Role role) {
        applicationAccessService.requireRecruiterForManagement(userId, role, appId);
        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        com.vthr.erp_hrm.infrastructure.controller.response.ApplicationResponse appResponse =
                ApplicationResponse.fromDomain(app);
        if (appResponse.getCvUrl() != null) {
            appResponse.setCvUrl(signedUrlService.generateSignedUrl("/api/files/cvs", app.getCvUrl()));
        }

        Job job = jobService.getJobById(app.getJobId());
        String jobTitle = job.getTitle();
        appResponse.setJobTitle(jobTitle);

        com.vthr.erp_hrm.core.model.User candidate = userRepository.findById(app.getCandidateId()).orElse(null);
        com.vthr.erp_hrm.infrastructure.controller.response.RecruiterCandidateProfile profile =
                com.vthr.erp_hrm.infrastructure.controller.response.RecruiterCandidateProfile.builder()
                        .id(app.getCandidateId())
                        .fullName(candidate != null ? candidate.getFullName() : null)
                        .email(candidate != null ? candidate.getEmail() : null)
                        .phone(candidate != null ? candidate.getPhone() : null)
                        .build();

        com.vthr.erp_hrm.core.model.AIEvaluation aiEval = aiEvaluationRepository.findByApplicationId(appId)
                .orElse(null);
        java.util.List<com.vthr.erp_hrm.core.model.Interview> interviews = interviewRepository
                .findByApplicationId(appId);

        return com.vthr.erp_hrm.infrastructure.controller.response.RecruiterApplicationReviewResponse.builder()
                .application(appResponse)
                .jobTitle(jobTitle)
                .candidate(profile)
                .interviews(interviews)
                .aiEvaluation(aiEval)
                .build();
    }

    @Override
    @Transactional
    public Application updateHrNote(UUID applicationId, String hrNote, UUID actorId, Role role) {
        applicationAccessService.requireRecruiterForManagement(actorId, role, applicationId);
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        String normalized = hrNote == null ? null : hrNote.trim();
        if (normalized != null && normalized.isEmpty()) {
            normalized = null;
        }
        if (normalized != null && normalized.length() > 8000) {
            throw new RuntimeException("HR note too long");
        }
        app.setHrNote(normalized);
        return applicationRepository.save(app);
    }

    @Override
    @Transactional
    public Application withdrawApplicationByCandidate(UUID applicationId, UUID candidateId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        if (!app.getCandidateId().equals(candidateId)) {
            throw new RuntimeException("Access Denied");
        }
        Job job = jobService.getJobById(app.getJobId());
        WithdrawalEligibilityResponse el = evaluateWithdrawalEligibility(applicationId, app, job);
        if (!el.isAllowed()) {
            throw new RuntimeException(el.getReason() != null ? el.getReason() : "Cannot withdraw application");
        }

        ApplicationStatus oldStatus = app.getStatus();
        app.setStatus(ApplicationStatus.WITHDRAWN);
        Application saved = applicationRepository.save(app);

        historyRepository.save(ApplicationStageHistory.builder()
                .applicationId(saved.getId())
                .fromStage(oldStatus)
                .toStage(ApplicationStatus.WITHDRAWN)
                .changedBy(candidateId)
                .note("Ứng viên rút đơn")
                .build());

        realtimeEventService.emitJobEvent(saved.getJobId(), "application:stage_changed", saved);
        return saved;
    }

    private WithdrawalEligibilityResponse evaluateWithdrawalEligibility(UUID applicationId, Application app, Job job) {
        if (app.getStatus() == ApplicationStatus.WITHDRAWN) {
            return WithdrawalEligibilityResponse.builder()
                    .allowed(false)
                    .reason("Đơn đã được rút trước đó.")
                    .build();
        }
        if (app.getStatus() == ApplicationStatus.REJECTED) {
            return WithdrawalEligibilityResponse.builder()
                    .allowed(false)
                    .reason("Không thể rút đơn đã bị từ chối.")
                    .build();
        }
        if (app.getStatus() == ApplicationStatus.HIRED) {
            return WithdrawalEligibilityResponse.builder()
                    .allowed(false)
                    .reason("Không thể rút đơn đã trúng tuyển.")
                    .build();
        }
        if (app.getStatus() == ApplicationStatus.INTERVIEW || app.getStatus() == ApplicationStatus.OFFER) {
            return WithdrawalEligibilityResponse.builder()
                    .allowed(false)
                    .reason("Không thể rút đơn ở giai đoạn phỏng vấn hoặc offer.")
                    .build();
        }
        if (!WITHDRAWABLE_STATUSES.contains(app.getStatus())) {
            return WithdrawalEligibilityResponse.builder()
                    .allowed(false)
                    .reason("Giai đoạn hiện tại không cho phép rút đơn.")
                    .build();
        }
        if (job.getStatus() != JobStatus.OPEN) {
            return WithdrawalEligibilityResponse.builder()
                    .allowed(false)
                    .reason("Tin tuyển dụng không còn mở.")
                    .build();
        }
        ZonedDateTime now = ZonedDateTime.now(clock);
        if (job.getExpiresAt() != null && !job.getExpiresAt().isAfter(now)) {
            return WithdrawalEligibilityResponse.builder()
                    .allowed(false)
                    .reason("Đã hết hạn nộp hồ sơ theo tin tuyển dụng.")
                    .build();
        }
        if (!interviewRepository.findByApplicationId(applicationId).isEmpty()) {
            return WithdrawalEligibilityResponse.builder()
                    .allowed(false)
                    .reason("Đã có lịch phỏng vấn — không thể rút đơn.")
                    .build();
        }
        return WithdrawalEligibilityResponse.builder().allowed(true).reason(null).build();
    }

    @Override
    public List<ApplicationStageHistory> getApplicationStageHistory(UUID applicationId, UUID userId, Role role) {
        applicationAccessService.requireParticipantForMessaging(userId, role, applicationId);
        return historyRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId);
    }
}
