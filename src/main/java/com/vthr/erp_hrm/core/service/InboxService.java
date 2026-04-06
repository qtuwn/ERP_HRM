package com.vthr.erp_hrm.core.service;

import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.infrastructure.controller.response.RecruiterInboxThreadResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface InboxService {

    Page<RecruiterInboxThreadResponse> listRecruiterThreads(
            UUID userId,
            Role role,
            UUID jobIdFilter,
            Pageable pageable);
}
