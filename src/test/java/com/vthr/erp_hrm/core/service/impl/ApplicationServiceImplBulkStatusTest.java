package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.AccountStatus;
import com.vthr.erp_hrm.core.model.Application;
import com.vthr.erp_hrm.core.model.ApplicationStatus;
import com.vthr.erp_hrm.core.model.Job;
import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.model.User;
import com.vthr.erp_hrm.core.repository.AIEvaluationRepository;
import com.vthr.erp_hrm.core.repository.ApplicationRepository;
import com.vthr.erp_hrm.core.repository.ApplicationStageHistoryRepository;
import com.vthr.erp_hrm.core.repository.InterviewRepository;
import com.vthr.erp_hrm.core.repository.JobRepository;
import com.vthr.erp_hrm.core.repository.MessageRepository;
import com.vthr.erp_hrm.core.repository.UserRepository;
import com.vthr.erp_hrm.core.service.ApplicationAccessService;
import com.vthr.erp_hrm.core.service.JobService;
import com.vthr.erp_hrm.infrastructure.email.EmailQueueService;
import com.vthr.erp_hrm.infrastructure.storage.SignedUrlService;
import com.vthr.erp_hrm.infrastructure.websocket.RealtimeEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplBulkStatusTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JobService jobService;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private EmailQueueService emailQueueService;
    @Mock
    private SignedUrlService signedUrlService;
    @Mock
    private AIEvaluationRepository aiEvaluationRepository;
    @Mock
    private InterviewRepository interviewRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private ApplicationStageHistoryRepository historyRepository;
    @Mock
    private ApplicationAccessService applicationAccessService;
    @Mock
    private RealtimeEventService realtimeEventService;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    @Test
    void bulkUpdateApplicationStatus_shouldReturnSucceededAndFailed() {
        UUID actorId = UUID.randomUUID();
        User actor = User.builder()
                .id(actorId)
                .role(Role.HR)
                .status(AccountStatus.ACTIVE)
                .isActive(true)
                .emailVerified(true)
                .build();
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));

        UUID okId = UUID.randomUUID();
        UUID badId = UUID.randomUUID();

        Application okApp = Application.builder()
                .id(okId)
                .jobId(UUID.randomUUID())
                .candidateId(UUID.randomUUID())
                .status(ApplicationStatus.APPLIED)
                .build();
        when(applicationRepository.findById(okId)).thenReturn(Optional.of(okApp));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jobService.getJobById(okApp.getJobId())).thenReturn(Job.builder().id(okApp.getJobId()).title("Dev").build());

        when(applicationRepository.findById(badId)).thenReturn(Optional.empty());

        doNothing().when(applicationAccessService).requireRecruiterForManagement(actorId, Role.HR, okId);

        var res = applicationService.bulkUpdateApplicationStatus(
                java.util.List.of(okId, badId),
                ApplicationStatus.REJECTED,
                actorId,
                "bulk"
        );

        assertEquals(1, res.getSucceededIds().size());
        assertEquals(okId, res.getSucceededIds().get(0));
        assertEquals(1, res.getFailed().size());

        verify(historyRepository, times(1)).save(any());
        verify(realtimeEventService, times(1)).emitJobEvent(any(), eq("application:stage_changed"), any());
    }
}

