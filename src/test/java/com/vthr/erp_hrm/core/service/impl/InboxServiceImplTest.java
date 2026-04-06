package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.Job;
import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.model.User;
import com.vthr.erp_hrm.core.service.ApplicationAccessService;
import com.vthr.erp_hrm.core.service.JobService;
import com.vthr.erp_hrm.core.service.UserService;
import com.vthr.erp_hrm.infrastructure.controller.response.RecruiterInboxThreadResponse;
import com.vthr.erp_hrm.infrastructure.persistence.repository.RecruiterInboxNativeQuery;
import com.vthr.erp_hrm.infrastructure.persistence.repository.RecruiterInboxRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboxServiceImplTest {

    @Mock
    private ApplicationAccessService applicationAccessService;
    @Mock
    private JobService jobService;
    @Mock
    private UserService userService;
    @Mock
    private RecruiterInboxNativeQuery recruiterInboxNativeQuery;

    @InjectMocks
    private InboxServiceImpl inboxService;

    @Test
    void listRecruiterThreads_withJobId_checksAccess_andQueriesNative() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        when(recruiterInboxNativeQuery.countByJobIds(List.of(jobId))).thenReturn(1L);
        RecruiterInboxRow row = new RecruiterInboxRow(
                appId,
                jobId,
                "Engineer",
                "Nam",
                "n@ex.com",
                "APPLIED",
                "hello",
                ZonedDateTime.now());
        when(recruiterInboxNativeQuery.fetchByJobIds(List.of(jobId), 0, 10)).thenReturn(List.of(row));

        Page<RecruiterInboxThreadResponse> page = inboxService.listRecruiterThreads(userId, Role.HR, jobId, pageable);

        verify(applicationAccessService).requireRecruiterForJobTopic(userId, Role.HR, jobId);
        assertEquals(1, page.getTotalElements());
        assertEquals(appId, page.getContent().getFirst().getApplicationId());
        assertEquals("hello", page.getContent().getFirst().getLastMessagePreview());
        verify(userService, never()).getUserById(any());
    }

    @Test
    void listRecruiterThreads_forbiddenJob_doesNotQueryInbox() {
        UUID jobId = UUID.randomUUID();
        doThrow(new RuntimeException("Access denied"))
                .when(applicationAccessService)
                .requireRecruiterForJobTopic(any(), any(), eq(jobId));

        assertThrows(
                RuntimeException.class,
                () -> inboxService.listRecruiterThreads(UUID.randomUUID(), Role.HR, jobId, PageRequest.of(0, 5)));

        verify(recruiterInboxNativeQuery, never()).countByJobIds(anyList());
    }

    @Test
    void listRecruiterThreads_hrWithoutCompany_returnsEmpty() {
        UUID userId = UUID.randomUUID();
        when(userService.getUserById(userId))
                .thenReturn(User.builder().id(userId).companyId(null).role(Role.HR).build());

        Page<RecruiterInboxThreadResponse> page =
                inboxService.listRecruiterThreads(userId, Role.HR, null, PageRequest.of(0, 10));

        assertTrue(page.isEmpty());
        verifyNoInteractions(recruiterInboxNativeQuery);
    }

    @Test
    void listRecruiterThreads_hrResolvesCreatedJobs_andQueries() {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID j1 = UUID.randomUUID();
        when(userService.getUserById(userId))
                .thenReturn(User.builder().id(userId).companyId(companyId).role(Role.HR).build());
        when(jobService.getJobsByCompanyIdAndCreatedBy(eq(companyId), eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(Job.builder().id(j1).build())));
        when(recruiterInboxNativeQuery.countByJobIds(List.of(j1))).thenReturn(0L);

        Page<RecruiterInboxThreadResponse> page =
                inboxService.listRecruiterThreads(userId, Role.HR, null, PageRequest.of(0, 20));

        assertTrue(page.isEmpty());
        verify(recruiterInboxNativeQuery).countByJobIds(List.of(j1));
    }

    @Test
    void listRecruiterThreads_companyUsesCompanyJobs() {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID j1 = UUID.randomUUID();
        when(userService.getUserById(userId))
                .thenReturn(User.builder().id(userId).companyId(companyId).role(Role.COMPANY).build());
        when(jobService.getJobsByCompanyId(eq(companyId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(Job.builder().id(j1).build())));
        when(recruiterInboxNativeQuery.countByJobIds(List.of(j1))).thenReturn(0L);

        inboxService.listRecruiterThreads(userId, Role.COMPANY, null, PageRequest.of(0, 10));

        verify(jobService, never()).getJobsByCompanyIdAndCreatedBy(any(), any(), any());
        verify(recruiterInboxNativeQuery).countByJobIds(List.of(j1));
    }

    @Test
    void listRecruiterThreads_admin_returnsEmpty() {
        Page<RecruiterInboxThreadResponse> page =
                inboxService.listRecruiterThreads(UUID.randomUUID(), Role.ADMIN, null, PageRequest.of(0, 5));

        assertTrue(page.isEmpty());
        verifyNoInteractions(recruiterInboxNativeQuery);
        verifyNoInteractions(jobService);
    }

    @Test
    void listRecruiterThreads_candidateRoleNoFilter_returnsEmpty() {
        Page<RecruiterInboxThreadResponse> page =
                inboxService.listRecruiterThreads(UUID.randomUUID(), Role.CANDIDATE, null, PageRequest.of(0, 10));

        assertTrue(page.isEmpty());
        verifyNoInteractions(recruiterInboxNativeQuery);
        verifyNoInteractions(userService);
    }

    @Test
    void listRecruiterThreads_truncatesLongPreview() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        String longBody = "a".repeat(250);
        when(recruiterInboxNativeQuery.countByJobIds(List.of(jobId))).thenReturn(1L);
        RecruiterInboxRow row = new RecruiterInboxRow(
                appId,
                jobId,
                "T",
                "U",
                "u@x.com",
                "APPLIED",
                longBody,
                null);
        when(recruiterInboxNativeQuery.fetchByJobIds(List.of(jobId), 0, 20)).thenReturn(List.of(row));

        Page<RecruiterInboxThreadResponse> page =
                inboxService.listRecruiterThreads(userId, Role.COMPANY, jobId, PageRequest.of(0, 20));

        String preview = page.getContent().getFirst().getLastMessagePreview();
        assertEquals(201, preview.length());
        assertTrue(preview.endsWith("…"));
    }
}
