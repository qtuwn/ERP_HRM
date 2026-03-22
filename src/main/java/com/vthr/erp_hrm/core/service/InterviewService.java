package com.vthr.erp_hrm.core.service;

import com.vthr.erp_hrm.core.model.Interview;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public interface InterviewService {
    Interview scheduleInterview(UUID applicationId, ZonedDateTime interviewTime, String locationOrLink, UUID interviewerId);
    List<Interview> getInterviewsByApplication(UUID applicationId);
    Interview updateInterviewStatus(UUID interviewId, String status);
}
