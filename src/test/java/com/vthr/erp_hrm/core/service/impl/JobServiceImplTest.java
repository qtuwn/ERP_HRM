package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.UUID;

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
        verify(jobRepository).findOpenJobsSearch("java", null, null, null, null, null, PageRequest.of(0, 10));
    }

    @Test
    void getJobsByCompanyIdAndCreatedBy_delegatesToRepository() {
        UUID cid = UUID.randomUUID();
        UUID uid = UUID.randomUUID();
        var pageable = PageRequest.of(0, 5);
        jobService.getJobsByCompanyIdAndCreatedBy(cid, uid, pageable);
        verify(jobRepository).findByCompanyIdAndCreatedBy(cid, uid, pageable);
    }
}

