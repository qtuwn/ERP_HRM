package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.*;
import com.vthr.erp_hrm.core.repository.ApplicationRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplInvalidStageChangeTest {

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

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    @Test
    void updateApplicationStatus_shouldRejectTransitionFromRejected() {
        UUID appId = UUID.randomUUID();
        UUID hrId = UUID.randomUUID();

        User hr = User.builder().id(hrId).role(Role.HR).emailVerified(true).status(AccountStatus.ACTIVE).isActive(true).build();
        when(userRepository.findById(hrId)).thenReturn(Optional.of(hr));

        Application app = Application.builder().id(appId).status(ApplicationStatus.REJECTED).jobId(UUID.randomUUID()).candidateId(UUID.randomUUID()).build();
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                applicationService.updateApplicationStatus(appId, ApplicationStatus.HR_REVIEW, hrId, "note")
        );
        assertEquals("Invalid status transition", ex.getMessage());

        verify(historyRepository, never()).save(any());
        verify(realtimeEventService, never()).emitJobEvent(any(), any(), any());
    }
}

