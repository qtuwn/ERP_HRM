package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.Application;
import com.vthr.erp_hrm.core.model.ApplicationStatus;
import com.vthr.erp_hrm.core.model.Job;
import com.vthr.erp_hrm.core.model.JobStatus;
import com.vthr.erp_hrm.core.repository.ApplicationRepository;
import com.vthr.erp_hrm.core.service.ApplicationAccessService;
import com.vthr.erp_hrm.core.service.JobService;
import com.vthr.erp_hrm.infrastructure.ai.AiQueueService;
import com.vthr.erp_hrm.infrastructure.email.EmailQueueService;
import com.vthr.erp_hrm.infrastructure.storage.SignedUrlService;
import com.vthr.erp_hrm.infrastructure.webhook.WebhookOutboxService;
import com.vthr.erp_hrm.infrastructure.websocket.RealtimeEventService;
import com.vthr.erp_hrm.core.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplNegativeTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private JobService jobService;
    @Mock
    private AiQueueService aiQueueService;
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
    private Clock clock;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    @BeforeEach
    void aiOff() {
        ReflectionTestUtils.setField(applicationService, "aiScreeningEnabled", false);
    }

    @Test
    void applyForJob_jobNotOpen_throws() {
        UUID jobId = UUID.randomUUID();
        when(jobService.getJobById(jobId)).thenReturn(Job.builder().id(jobId).status(JobStatus.CLOSED).build());

        assertThrows(RuntimeException.class, () -> applicationService.applyForJob(jobId, UUID.randomUUID(), "cv.pdf"));
    }

    @Test
    void applyForJob_duplicateApply_throws() {
        UUID jobId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        when(jobService.getJobById(jobId)).thenReturn(Job.builder().id(jobId).status(JobStatus.OPEN).build());
        when(applicationRepository.findByJobIdAndCandidateId(jobId, candidateId))
                .thenReturn(Optional.of(Application.builder()
                        .id(UUID.randomUUID())
                        .jobId(jobId)
                        .candidateId(candidateId)
                        .status(ApplicationStatus.APPLIED)
                        .build()));

        assertThrows(RuntimeException.class, () -> applicationService.applyForJob(jobId, candidateId, "cv.pdf"));
    }
}

