package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.Job;
import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.model.User;
import com.vthr.erp_hrm.core.service.ApplicationAccessService;
import com.vthr.erp_hrm.core.service.InboxService;
import com.vthr.erp_hrm.core.service.JobService;
import com.vthr.erp_hrm.core.service.UserService;
import com.vthr.erp_hrm.infrastructure.controller.response.RecruiterInboxThreadResponse;
import com.vthr.erp_hrm.infrastructure.persistence.repository.RecruiterInboxNativeQuery;
import com.vthr.erp_hrm.infrastructure.persistence.repository.RecruiterInboxRow;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InboxServiceImpl implements InboxService {

    private static final int MAX_JOBS_SCOPE = 2000;
    private static final int PREVIEW_MAX = 200;

    private final ApplicationAccessService applicationAccessService;
    private final JobService jobService;
    private final UserService userService;
    private final RecruiterInboxNativeQuery recruiterInboxNativeQuery;

    @Override
    public Page<RecruiterInboxThreadResponse> listRecruiterThreads(
            UUID userId,
            Role role,
            UUID jobIdFilter,
            Pageable pageable) {
        List<UUID> jobIds = resolveJobIds(userId, role, jobIdFilter);
        if (jobIds.isEmpty()) {
            return Page.empty(pageable);
        }
        long total = recruiterInboxNativeQuery.countByJobIds(jobIds);
        if (total == 0) {
            return Page.empty(pageable);
        }
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();
        List<RecruiterInboxRow> rows = recruiterInboxNativeQuery.fetchByJobIds(jobIds, offset, limit);
        List<RecruiterInboxThreadResponse> content = new ArrayList<>(rows.size());
        for (RecruiterInboxRow r : rows) {
            content.add(RecruiterInboxThreadResponse.builder()
                    .applicationId(r.applicationId())
                    .jobId(r.jobId())
                    .jobTitle(r.jobTitle())
                    .candidateName(r.candidateName())
                    .candidateEmail(r.candidateEmail())
                    .status(r.status())
                    .lastMessagePreview(truncatePreview(r.lastMessageContent()))
                    .lastMessageAt(r.lastMessageAt())
                    .build());
        }
        return new PageImpl<>(content, pageable, total);
    }

    private List<UUID> resolveJobIds(UUID userId, Role role, UUID jobIdFilter) {
        if (jobIdFilter != null) {
            applicationAccessService.requireRecruiterForJobTopic(userId, role, jobIdFilter);
            return List.of(jobIdFilter);
        }
        Pageable unpagedChunk = PageRequest.of(0, MAX_JOBS_SCOPE);
        if (role != Role.HR && role != Role.COMPANY) {
            return List.of();
        }
        User user = userService.getUserById(userId);
        if (user.getCompanyId() == null) {
            return List.of();
        }
        if (role == Role.COMPANY) {
            return jobService.getJobsByCompanyId(user.getCompanyId(), unpagedChunk).getContent().stream()
                    .map(Job::getId)
                    .toList();
        }
        if (role == Role.HR) {
            return jobService
                    .getJobsByCompanyIdAndCreatedBy(user.getCompanyId(), userId, unpagedChunk)
                    .getContent()
                    .stream()
                    .map(Job::getId)
                    .toList();
        }
        return List.of();
    }

    private static String truncatePreview(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        String t = content.replace('\n', ' ').trim();
        if (t.length() <= PREVIEW_MAX) {
            return t;
        }
        return t.substring(0, PREVIEW_MAX) + "…";
    }
}
