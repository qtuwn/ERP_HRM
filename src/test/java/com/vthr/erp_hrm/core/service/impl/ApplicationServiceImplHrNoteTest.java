package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.Application;
import com.vthr.erp_hrm.core.model.ApplicationStatus;
import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.repository.ApplicationRepository;
import com.vthr.erp_hrm.core.service.ApplicationAccessService;
import com.vthr.erp_hrm.core.service.JobService;
import com.vthr.erp_hrm.infrastructure.ai.AiQueueService;
import com.vthr.erp_hrm.infrastructure.email.EmailQueueService;
import com.vthr.erp_hrm.infrastructure.storage.SignedUrlService;
import com.vthr.erp_hrm.infrastructure.webhook.WebhookOutboxService;
import com.vthr.erp_hrm.infrastructure.websocket.RealtimeEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplHrNoteTest {

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
    private com.vthr.erp_hrm.core.service.NotificationService notificationService;
    @Mock
    private Clock clock;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    @BeforeEach
    void aiOff() {
        ReflectionTestUtils.setField(applicationService, "aiScreeningEnabled", false);
    }

    @Test
    void updateHrNote_trimsAndSaves() {
        UUID appId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID hrId = UUID.randomUUID();
        UUID candId = UUID.randomUUID();

        Application app = Application.builder()
                .id(appId)
                .jobId(jobId)
                .candidateId(candId)
                .status(ApplicationStatus.HR_REVIEW)
                .cvUrl("cvs/x.pdf")
                .build();
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        applicationService.updateHrNote(appId, "  Ghi chú nội bộ  ", hrId, Role.HR);

        verify(applicationAccessService).requireRecruiterForManagement(eq(hrId), eq(Role.HR), eq(appId));
        ArgumentCaptor<Application> cap = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(cap.capture());
        assertEquals("Ghi chú nội bộ", cap.getValue().getHrNote());
    }

    @Test
    void updateHrNote_blankClearsNote() {
        UUID appId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID hrId = UUID.randomUUID();

        Application app = Application.builder()
                .id(appId)
                .jobId(jobId)
                .candidateId(UUID.randomUUID())
                .status(ApplicationStatus.HR_REVIEW)
                .hrNote("old")
                .build();
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        applicationService.updateHrNote(appId, "   ", hrId, Role.HR);

        ArgumentCaptor<Application> cap = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(cap.capture());
        assertNull(cap.getValue().getHrNote());
    }

    @Test
    void updateHrNote_tooLong_throws() {
        UUID appId = UUID.randomUUID();
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(
                Application.builder()
                        .id(appId)
                        .jobId(UUID.randomUUID())
                        .candidateId(UUID.randomUUID())
                        .status(ApplicationStatus.HR_REVIEW)
                        .build()));

        String longNote = "x".repeat(8001);
        UUID hrId = UUID.randomUUID();
        assertThrows(RuntimeException.class, () ->
                applicationService.updateHrNote(appId, longNote, hrId, Role.HR));
    }
}
