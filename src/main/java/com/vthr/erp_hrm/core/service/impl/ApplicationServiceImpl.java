package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.Application;
import com.vthr.erp_hrm.core.model.ApplicationStatus;
import com.vthr.erp_hrm.core.model.Job;
import com.vthr.erp_hrm.core.model.JobStatus;
import com.vthr.erp_hrm.core.repository.ApplicationRepository;
import com.vthr.erp_hrm.core.service.ApplicationService;
import com.vthr.erp_hrm.core.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobService jobService;
    private final com.vthr.erp_hrm.infrastructure.ai.AiQueueService aiQueueService;
    private final com.vthr.erp_hrm.core.repository.ApplicationStageHistoryRepository historyRepository;
    private final com.vthr.erp_hrm.core.repository.UserRepository userRepository;
    private final com.vthr.erp_hrm.core.repository.AIEvaluationRepository aiEvaluationRepository;

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
                .build();
        Application saved = applicationRepository.save(application);
        aiQueueService.enqueueApplication(saved.getId());
        return saved;
    }

    @Override
    public java.util.List<com.vthr.erp_hrm.infrastructure.controller.response.KanbanApplicationResponse> getKanbanApplications(UUID jobId) {
        return applicationRepository.findByJobId(jobId).stream().map(app -> {
            com.vthr.erp_hrm.core.model.User candidate = userRepository.findById(app.getCandidateId()).orElse(null);
            String candidateName = candidate != null ? candidate.getFullName() : "Unknown";
            String candidateEmail = candidate != null ? candidate.getEmail() : "Unknown";
            
            Integer aiScore = null;
            com.vthr.erp_hrm.core.model.AIEvaluation eval = aiEvaluationRepository.findByApplicationId(app.getId()).orElse(null);
            if (eval != null) {
                aiScore = eval.getScore();
            }
            
            return com.vthr.erp_hrm.infrastructure.controller.response.KanbanApplicationResponse.builder()
                    .id(app.getId())
                    .candidateId(app.getCandidateId())
                    .candidateName(candidateName)
                    .candidateEmail(candidateEmail)
                    .status(app.getStatus() != null ? app.getStatus().name() : null)
                    .aiStatus(app.getAiStatus())
                    .aiScore(aiScore)
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
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        
        ApplicationStatus oldStatus = application.getStatus();
        if (oldStatus == status) return application;
        
        application.setStatus(status);
        Application saved = applicationRepository.save(application);
        
        historyRepository.save(com.vthr.erp_hrm.core.model.ApplicationStageHistory.builder()
                .applicationId(saved.getId())
                .fromStage(oldStatus)
                .toStage(status)
                .changedBy(changedBy)
                .note(note)
                .build());
                
        return saved;
    }
}
