package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.Application;
import com.vthr.erp_hrm.core.model.ApplicationStatus;
import com.vthr.erp_hrm.core.model.Interview;
import com.vthr.erp_hrm.core.model.Job;
import com.vthr.erp_hrm.core.model.User;
import com.vthr.erp_hrm.core.repository.ApplicationRepository;
import com.vthr.erp_hrm.core.repository.InterviewRepository;
import com.vthr.erp_hrm.core.repository.JobRepository;
import com.vthr.erp_hrm.core.repository.UserRepository;
import com.vthr.erp_hrm.core.service.ApplicationService;
import com.vthr.erp_hrm.core.service.InterviewService;
import com.vthr.erp_hrm.infrastructure.mail.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final EmailNotificationService emailService;
    private final ApplicationService applicationService;

    @Override
    public Interview scheduleInterview(UUID applicationId, ZonedDateTime interviewTime, String locationOrLink, UUID interviewerId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
                
        Interview interview = Interview.builder()
                .applicationId(applicationId)
                .interviewTime(interviewTime)
                .locationOrLink(locationOrLink)
                .interviewerId(interviewerId)
                .status("SCHEDULED")
                .build();
                
        Interview saved = interviewRepository.save(interview);
        
        applicationService.updateApplicationStatus(applicationId, ApplicationStatus.INTERVIEW, interviewerId, "Scheduled Interview");

        Job job = jobRepository.findById(application.getJobId()).orElse(null);
        User candidate = userRepository.findById(application.getCandidateId()).orElse(null);
        
        if (job != null && candidate != null) {
            String timeStr = interviewTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm z"));
            emailService.sendInterviewInvitation(
                    candidate.getEmail(),
                    candidate.getFullName(),
                    job.getTitle(),
                    timeStr,
                    locationOrLink
            );
        }

        return saved;
    }

    @Override
    public List<Interview> getInterviewsByApplication(UUID applicationId) {
        return interviewRepository.findByApplicationId(applicationId);
    }

    @Override
    public Interview updateInterviewStatus(UUID interviewId, String status) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview not found"));
        interview.setStatus(status);
        return interviewRepository.save(interview);
    }
}
