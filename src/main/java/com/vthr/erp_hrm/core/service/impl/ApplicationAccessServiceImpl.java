package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.Application;
import com.vthr.erp_hrm.core.model.Job;
import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.model.User;
import com.vthr.erp_hrm.core.repository.ApplicationRepository;
import com.vthr.erp_hrm.core.repository.JobRepository;
import com.vthr.erp_hrm.core.repository.UserRepository;
import com.vthr.erp_hrm.core.service.ApplicationAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationAccessServiceImpl implements ApplicationAccessService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    @Override
    public void requireParticipantForMessaging(UUID userId, Role role, UUID applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        if (role == Role.ADMIN) {
            throw new RuntimeException("Access denied");
        }
        if (role == Role.CANDIDATE) {
            if (!application.getCandidateId().equals(userId)) {
                throw new RuntimeException("Access denied");
            }
            return;
        }
        if (role == Role.HR || role == Role.COMPANY) {
            requireRecruiterAccessToJob(userId, role, application.getJobId());
            return;
        }
        throw new RuntimeException("Access denied");
    }

    @Override
    public void requireRecruiterForManagement(UUID userId, Role role, UUID applicationId) {
        if (role == Role.CANDIDATE) {
            throw new RuntimeException("Access denied");
        }
        if (role == Role.ADMIN) {
            throw new RuntimeException("Access denied");
        }
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        if (role == Role.HR || role == Role.COMPANY) {
            requireRecruiterAccessToJob(userId, role, application.getJobId());
            return;
        }
        throw new RuntimeException("Access denied");
    }

    @Override
    public void requireRecruiterForJobTopic(UUID userId, Role role, UUID jobId) {
        if (role == Role.CANDIDATE) {
            throw new RuntimeException("Access denied");
        }
        if (role == Role.ADMIN) {
            throw new RuntimeException("Access denied");
        }
        if (role == Role.HR || role == Role.COMPANY) {
            requireRecruiterAccessToJob(userId, role, jobId);
            return;
        }
        throw new RuntimeException("Access denied");
    }

    /**
     * COMPANY: cùng {@code companyId} với job. HR: thêm phải là người tạo job ({@code createdBy}).
     */
    private void requireRecruiterAccessToJob(UUID userId, Role role, UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getCompanyId() == null || job.getCompanyId() == null
                || !user.getCompanyId().equals(job.getCompanyId())) {
            throw new RuntimeException("Access denied");
        }
        if (role == Role.HR) {
            if (job.getCreatedBy() == null || !job.getCreatedBy().equals(userId)) {
                throw new RuntimeException("Access denied");
            }
        }
    }
}
