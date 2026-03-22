package com.vthr.erp_hrm.infrastructure.scheduler;

import com.vthr.erp_hrm.core.model.Job;
import com.vthr.erp_hrm.core.model.JobStatus;
import com.vthr.erp_hrm.core.model.User;
import com.vthr.erp_hrm.core.repository.JobRepository;
import com.vthr.erp_hrm.core.repository.UserRepository;
import com.vthr.erp_hrm.infrastructure.email.EmailQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class JobExpiryScheduler {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final EmailQueueService emailQueueService;

    // Run every hour at minute 0
    @Scheduled(cron = "0 0 * * * *")
    public void autoCloseExpiredJobs() {
        log.info("Running Job Expiry Scheduler at {}", ZonedDateTime.now());
        List<Job> expiredJobs = jobRepository.findByStatusAndExpiresAtBefore(
                JobStatus.OPEN.name(), ZonedDateTime.now());

        if (expiredJobs.isEmpty()) {
            return;
        }

        log.info("Found {} expired jobs to close.", expiredJobs.size());

        for (Job job : expiredJobs) {
            job.setStatus(JobStatus.CLOSED);
            job.setUpdatedAt(ZonedDateTime.now());
            jobRepository.save(job);

            log.info("Automatically closed job ID: {}", job.getId());

            User hrUser = userRepository.findById(job.getCreatedBy()).orElse(null);
            if (hrUser != null) {
                Map<String, Object> vars = new HashMap<>();
                vars.put("jobTitle", job.getTitle());
                vars.put("userName", hrUser.getFullName());
                
                emailQueueService.enqueueEmail(
                        hrUser.getEmail(),
                        "System Alert: Job Posting Expired & Closed",
                        "email/job-expired",
                        vars
                );
            }
        }
    }
}
