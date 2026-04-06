package com.vthr.erp_hrm.infrastructure.scheduler;

import com.vthr.erp_hrm.core.model.Job;
import com.vthr.erp_hrm.core.model.JobStatus;
import com.vthr.erp_hrm.core.model.User;
import com.vthr.erp_hrm.core.repository.JobRepository;
import com.vthr.erp_hrm.core.repository.UserRepository;
import com.vthr.erp_hrm.infrastructure.email.EmailQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobExpirySchedulerTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailQueueService emailQueueService;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache publicJobCache;

    private final Clock clock = Clock.fixed(Instant.parse("2026-04-01T00:00:00Z"), ZoneOffset.UTC);

    private JobExpiryScheduler scheduler;

    @BeforeEach
    void setup() {
        when(cacheManager.getCache(anyString())).thenReturn(publicJobCache);
        this.scheduler = new JobExpiryScheduler(jobRepository, userRepository, emailQueueService, cacheManager, clock);
        ReflectionTestUtils.setField(scheduler, "expiryEmailEnabled", false);
    }

    @Test
    void autoCloseExpiredJobs_shouldCloseJobsAndNotSendEmailWhenDisabled() {
        UUID jobId = UUID.randomUUID();
        UUID hrId = UUID.randomUUID();

        Job expired = Job.builder()
                .id(jobId)
                .title("Dev")
                .createdBy(hrId)
                .status(JobStatus.OPEN)
                .build();

        ZonedDateTime now = ZonedDateTime.now(clock);
        when(jobRepository.findByStatusAndExpiresAtBefore(eq("OPEN"), eq(now))).thenReturn(List.of(expired));

        scheduler.autoCloseExpiredJobs();

        ArgumentCaptor<Job> cap = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(cap.capture());
        assertEquals(JobStatus.CLOSED, cap.getValue().getStatus());
        assertEquals(now, cap.getValue().getUpdatedAt());

        verify(userRepository, never()).findById(any());
        verify(emailQueueService, never()).enqueueEmail(any(), any(), any(), any());
    }

    @Test
    void autoCloseExpiredJobs_shouldSendEmailWhenEnabledAndHrFound() {
        ReflectionTestUtils.setField(scheduler, "expiryEmailEnabled", true);

        UUID jobId = UUID.randomUUID();
        UUID hrId = UUID.randomUUID();

        Job expired = Job.builder()
                .id(jobId)
                .title("Dev")
                .createdBy(hrId)
                .status(JobStatus.OPEN)
                .build();

        User hr = User.builder()
                .id(hrId)
                .email("hr@ex.com")
                .fullName("HR")
                .build();

        ZonedDateTime now = ZonedDateTime.now(clock);
        when(jobRepository.findByStatusAndExpiresAtBefore(eq("OPEN"), eq(now))).thenReturn(List.of(expired));
        when(userRepository.findById(hrId)).thenReturn(Optional.of(hr));

        scheduler.autoCloseExpiredJobs();

        verify(jobRepository).save(any(Job.class));
        verify(userRepository).findById(hrId);
        verify(emailQueueService).enqueueEmail(eq("hr@ex.com"), any(), eq("email/job-expired"), any());
    }
}

