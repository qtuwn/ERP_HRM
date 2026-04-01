package com.vthr.erp_hrm.core.service;

import com.vthr.erp_hrm.core.model.Interview;
import com.vthr.erp_hrm.core.model.Role;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public interface InterviewService {
    Interview scheduleInterview(UUID applicationId, ZonedDateTime interviewTime, String locationOrLink, UUID interviewerId);
    List<Interview> getInterviewsByApplication(UUID applicationId, UUID viewerId, Role viewerRole);
    Interview updateInterviewStatus(UUID interviewId, String status, UUID actorId, Role actorRole);
}
