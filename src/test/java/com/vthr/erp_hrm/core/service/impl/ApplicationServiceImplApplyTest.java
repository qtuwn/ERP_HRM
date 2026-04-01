package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.Application;
import com.vthr.erp_hrm.core.model.ApplicationStatus;
import com.vthr.erp_hrm.core.model.Job;
import com.vthr.erp_hrm.core.model.JobStatus;
import com.vthr.erp_hrm.core.repository.ApplicationRepository;
import com.vthr.erp_hrm.core.repository.UserRepository;
import com.vthr.erp_hrm.core.service.ApplicationAccessService;
import com.vthr.erp_hrm.core.service.JobService;
import com.vthr.erp_hrm.infrastructure.ai.AiQueueService;
import com.vthr.erp_hrm.infrastructure.email.EmailQueueService;
import com.vthr.erp_hrm.infrastructure.storage.SignedUrlService;
import com.vthr.erp_hrm.infrastructure.websocket.RealtimeEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplApplyTest {

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
    private UserRepository userRepository;
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

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    @BeforeEach
    void aiOff() {
        ReflectionTestUtils.setField(applicationService, "aiScreeningEnabled", false);
    }

    @Test
    void applyForJob_aiDisabled_doesNotEnqueue_setsDisabledStatus() {
        UUID jobId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        when(jobService.getJobById(jobId)).thenReturn(Job.builder().id(jobId).status(JobStatus.OPEN).title("T").build());
        when(applicationRepository.existsByJobIdAndCandidateId(jobId, candidateId)).thenReturn(false);
        UUID savedId = UUID.randomUUID();
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> {
            Application a = inv.getArgument(0);
            a.setId(savedId);
            return a;
        });

        applicationService.applyForJob(jobId, candidateId, "cv.pdf");

        verify(aiQueueService, never()).enqueueApplication(any());
        ArgumentCaptor<Application> cap = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(cap.capture());
        assertEquals("DISABLED", cap.getValue().getAiStatus());
        assertEquals(ApplicationStatus.APPLIED, cap.getValue().getStatus());
    }

    @Test
    void applyForJob_aiEnabled_enqueues() {
        ReflectionTestUtils.setField(applicationService, "aiScreeningEnabled", true);
        UUID jobId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        when(jobService.getJobById(jobId)).thenReturn(Job.builder().id(jobId).status(JobStatus.OPEN).title("T").build());
        when(applicationRepository.existsByJobIdAndCandidateId(jobId, candidateId)).thenReturn(false);
        UUID savedId = UUID.randomUUID();
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> {
            Application a = inv.getArgument(0);
            a.setId(savedId);
            return a;
        });

        applicationService.applyForJob(jobId, candidateId, "cv.pdf");

        verify(aiQueueService).enqueueApplication(savedId);
        ArgumentCaptor<Application> cap = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(cap.capture());
        assertEquals("AI_QUEUED", cap.getValue().getAiStatus());
    }
}
