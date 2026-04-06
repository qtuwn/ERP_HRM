package com.vthr.erp_hrm.core.service;

import com.vthr.erp_hrm.core.model.ApplicationTask;
import com.vthr.erp_hrm.core.model.ApplicationTaskDocumentType;
import com.vthr.erp_hrm.core.model.ApplicationTaskStatus;
import com.vthr.erp_hrm.core.model.Role;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public interface ApplicationTaskService {

    List<ApplicationTask> listTasks(UUID applicationId, UUID userId, Role role);

    ApplicationTask getTask(UUID applicationId, UUID taskId, UUID userId, Role role);

    ApplicationTask createTask(
            UUID applicationId,
            UUID recruiterUserId,
            Role role,
            String title,
            String description,
            ApplicationTaskDocumentType documentType,
            ZonedDateTime dueAt
    );

    ApplicationTask uploadAttachment(
            UUID applicationId,
            UUID taskId,
            UUID candidateUserId,
            Role role,
            MultipartFile file
    ) throws IOException;

    ApplicationTask reviewTask(
            UUID applicationId,
            UUID taskId,
            UUID recruiterUserId,
            Role role,
            ApplicationTaskStatus status,
            String hrFeedback
    );

    void deleteTask(UUID applicationId, UUID taskId, UUID recruiterUserId, Role role);

    void deleteAttachment(UUID applicationId, UUID taskId, UUID attachmentId, UUID userId, Role role);
}
