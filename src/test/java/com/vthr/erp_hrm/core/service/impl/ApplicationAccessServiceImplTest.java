package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.Application;
import com.vthr.erp_hrm.core.model.Job;
import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.model.User;
import com.vthr.erp_hrm.core.repository.ApplicationRepository;
import com.vthr.erp_hrm.core.repository.JobRepository;
import com.vthr.erp_hrm.core.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationAccessServiceImplTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ApplicationAccessServiceImpl accessService;

    @Test
    void participantMessaging_candidateOwner_ok() {
        UUID appId = UUID.randomUUID();
        UUID candId = UUID.randomUUID();
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(
                Application.builder().id(appId).candidateId(candId).jobId(UUID.randomUUID()).build()));

        assertDoesNotThrow(() -> accessService.requireParticipantForMessaging(candId, Role.CANDIDATE, appId));
    }

    @Test
    void participantMessaging_candidateOther_denied() {
        UUID appId = UUID.randomUUID();
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(
                Application.builder().id(appId).candidateId(UUID.randomUUID()).jobId(UUID.randomUUID()).build()));

        assertThrows(RuntimeException.class,
                () -> accessService.requireParticipantForMessaging(UUID.randomUUID(), Role.CANDIDATE, appId));
    }

    @Test
    void participantMessaging_hrSameCompany_jobCreator_ok() {
        UUID appId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID hrId = UUID.randomUUID();
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(
                Application.builder().id(appId).candidateId(UUID.randomUUID()).jobId(jobId).build()));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(
                Job.builder().id(jobId).companyId(companyId).createdBy(hrId).build()));
        when(userRepository.findById(hrId)).thenReturn(Optional.of(User.builder().id(hrId).companyId(companyId).role(Role.HR).build()));

        assertDoesNotThrow(() -> accessService.requireParticipantForMessaging(hrId, Role.HR, appId));
    }

    @Test
    void participantMessaging_hrSameCompany_notJobCreator_denied() {
        UUID appId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID hrId = UUID.randomUUID();
        UUID otherHr = UUID.randomUUID();
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(
                Application.builder().id(appId).candidateId(UUID.randomUUID()).jobId(jobId).build()));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(
                Job.builder().id(jobId).companyId(companyId).createdBy(otherHr).build()));
        when(userRepository.findById(hrId)).thenReturn(Optional.of(User.builder().id(hrId).companyId(companyId).role(Role.HR).build()));

        assertThrows(RuntimeException.class, () -> accessService.requireParticipantForMessaging(hrId, Role.HR, appId));
    }

    @Test
    void participantMessaging_hrOtherCompany_denied() {
        UUID appId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID hrId = UUID.randomUUID();
        UUID jobCompany = UUID.randomUUID();
        UUID hrCompany = UUID.randomUUID();
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(
                Application.builder().id(appId).candidateId(UUID.randomUUID()).jobId(jobId).build()));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(Job.builder().id(jobId).companyId(jobCompany).build()));
        when(userRepository.findById(hrId)).thenReturn(Optional.of(
                User.builder().id(hrId).companyId(hrCompany).role(Role.HR).build()));

        assertThrows(RuntimeException.class, () -> accessService.requireParticipantForMessaging(hrId, Role.HR, appId));
    }

    @Test
    void recruiterManagement_candidate_denied() {
        assertThrows(RuntimeException.class,
                () -> accessService.requireRecruiterForManagement(UUID.randomUUID(), Role.CANDIDATE, UUID.randomUUID()));
    }

    @Test
    void jobTopic_candidate_denied() {
        assertThrows(RuntimeException.class,
                () -> accessService.requireRecruiterForJobTopic(UUID.randomUUID(), Role.CANDIDATE, UUID.randomUUID()));
    }

    @Test
    void jobTopic_company_sameCompany_ok_withoutBeingCreator() {
        UUID jobId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID companyUserId = UUID.randomUUID();
        UUID otherHrId = UUID.randomUUID();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(
                Job.builder().id(jobId).companyId(companyId).createdBy(otherHrId).build()));
        when(userRepository.findById(companyUserId)).thenReturn(Optional.of(
                User.builder().id(companyUserId).companyId(companyId).role(Role.COMPANY).build()));

        assertDoesNotThrow(() -> accessService.requireRecruiterForJobTopic(companyUserId, Role.COMPANY, jobId));
    }

    @Test
    void jobTopic_hr_notCreator_denied() {
        UUID jobId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID hrId = UUID.randomUUID();
        UUID otherHrId = UUID.randomUUID();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(
                Job.builder().id(jobId).companyId(companyId).createdBy(otherHrId).build()));
        when(userRepository.findById(hrId)).thenReturn(Optional.of(
                User.builder().id(hrId).companyId(companyId).role(Role.HR).build()));

        assertThrows(RuntimeException.class, () -> accessService.requireRecruiterForJobTopic(hrId, Role.HR, jobId));
    }
}
