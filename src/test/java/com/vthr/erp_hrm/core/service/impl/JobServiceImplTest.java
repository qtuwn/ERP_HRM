package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private JobServiceImpl jobService;

    @Test
    void getOpenJobs_delegatesToRepository() {
        jobService.getOpenJobs("java", PageRequest.of(0, 10));
        verify(jobRepository).findOpenJobsWithOptionalKeyword("java", PageRequest.of(0, 10));
    }
}

