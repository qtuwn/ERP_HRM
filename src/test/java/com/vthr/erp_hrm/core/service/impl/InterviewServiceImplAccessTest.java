package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.Interview;
import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.repository.ApplicationRepository;
import com.vthr.erp_hrm.core.repository.InterviewRepository;
import com.vthr.erp_hrm.core.repository.JobRepository;
import com.vthr.erp_hrm.core.repository.UserRepository;
import com.vthr.erp_hrm.core.service.ApplicationAccessService;
import com.vthr.erp_hrm.core.service.ApplicationService;
import com.vthr.erp_hrm.infrastructure.email.EmailQueueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewServiceImplAccessTest {

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailQueueService emailQueueService;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private ApplicationAccessService applicationAccessService;

    @InjectMocks
    private InterviewServiceImpl interviewService;

    @Test
    void getInterviewsByApplication_shouldRequireParticipant() {
        UUID appId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();

        doNothing().when(applicationAccessService).requireParticipantForMessaging(viewerId, Role.CANDIDATE, appId);
        when(interviewRepository.findByApplicationId(appId)).thenReturn(List.of());

        List<Interview> res = interviewService.getInterviewsByApplication(appId, viewerId, Role.CANDIDATE);

        assertEquals(0, res.size());
        verify(applicationAccessService).requireParticipantForMessaging(eq(viewerId), eq(Role.CANDIDATE), eq(appId));
    }

    @Test
    void updateInterviewStatus_shouldRequireRecruiter() {
        UUID interviewId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        Interview existing = Interview.builder().id(interviewId).applicationId(appId).status("SCHEDULED").build();
        when(interviewRepository.findById(interviewId)).thenReturn(Optional.of(existing));
        doNothing().when(applicationAccessService).requireRecruiterForManagement(actorId, Role.HR, appId);
        when(interviewRepository.save(any(Interview.class))).thenAnswer(inv -> inv.getArgument(0));

        Interview updated = interviewService.updateInterviewStatus(interviewId, "DONE", actorId, Role.HR);

        assertEquals("DONE", updated.getStatus());
        verify(applicationAccessService).requireRecruiterForManagement(eq(actorId), eq(Role.HR), eq(appId));
    }
}

