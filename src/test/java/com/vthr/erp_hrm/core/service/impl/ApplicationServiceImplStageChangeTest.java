package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.*;
import com.vthr.erp_hrm.core.repository.ApplicationRepository;
import com.vthr.erp_hrm.core.service.ApplicationAccessService;
import com.vthr.erp_hrm.core.service.JobService;
import com.vthr.erp_hrm.infrastructure.email.EmailQueueService;
import com.vthr.erp_hrm.infrastructure.storage.SignedUrlService;
import com.vthr.erp_hrm.infrastructure.websocket.RealtimeEventService;
import com.vthr.erp_hrm.core.service.NotificationService;
import com.vthr.erp_hrm.infrastructure.webhook.WebhookOutboxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplStageChangeTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private JobService jobService;
    @Mock
    private com.vthr.erp_hrm.infrastructure.ai.AiQueueService aiQueueService;
    @Mock
    private ApplicationAccessService applicationAccessService;
    @Mock
    private com.vthr.erp_hrm.core.repository.ApplicationStageHistoryRepository historyRepository;
    @Mock
    private com.vthr.erp_hrm.core.repository.UserRepository userRepository;
    @Mock
    private com.vthr.erp_hrm.core.repository.AIEvaluationRepository aiEvaluationRepository;
    @Mock
    private com.vthr.erp_hrm.core.repository.InterviewRepository interviewRepository;
    @Mock
    private RealtimeEventService realtimeEventService;
    @Mock
    private EmailQueueService emailQueueService;
    @Mock
    private SignedUrlService signedUrlService;
    @Mock
    private WebhookOutboxService webhookOutboxService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private java.time.Clock clock;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    @Test
    void updateApplicationStatus_savesAuditAndEmitsJobEvent() {
        UUID appId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        UUID hrId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        User hr = User.builder().id(hrId).role(Role.HR).companyId(companyId).emailVerified(true).status(AccountStatus.ACTIVE).isActive(true).build();
        when(userRepository.findById(hrId)).thenReturn(Optional.of(hr));

        Application app = Application.builder()
                .id(appId)
                .jobId(jobId)
                .candidateId(candidateId)
                .status(ApplicationStatus.APPLIED)
                .build();
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        when(jobService.getJobById(jobId)).thenReturn(Job.builder().id(jobId).title("T").status(JobStatus.OPEN).build());
        when(userRepository.findById(candidateId)).thenReturn(Optional.empty());

        applicationService.updateApplicationStatus(appId, ApplicationStatus.HR_REVIEW, hrId, "note");

        verify(applicationAccessService).requireRecruiterForManagement(hrId, Role.HR, appId);

        ArgumentCaptor<ApplicationStageHistory> histCap = ArgumentCaptor.forClass(ApplicationStageHistory.class);
        verify(historyRepository).save(histCap.capture());
        assertEquals(appId, histCap.getValue().getApplicationId());
        assertEquals(ApplicationStatus.APPLIED, histCap.getValue().getFromStage());
        assertEquals(ApplicationStatus.HR_REVIEW, histCap.getValue().getToStage());
        assertEquals(hrId, histCap.getValue().getChangedBy());
        assertEquals("note", histCap.getValue().getNote());

        verify(realtimeEventService).emitJobEvent(eq(jobId), eq("application:stage_changed"), any());
    }
}

