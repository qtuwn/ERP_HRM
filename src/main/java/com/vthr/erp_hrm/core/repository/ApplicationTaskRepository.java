package com.vthr.erp_hrm.core.repository;

import com.vthr.erp_hrm.core.model.ApplicationTask;
import com.vthr.erp_hrm.core.model.ApplicationTaskAttachment;
import com.vthr.erp_hrm.core.model.ApplicationTaskStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationTaskRepository {

    ApplicationTask createTask(ApplicationTask task);

    Optional<ApplicationTask> findDetailById(UUID id);

    List<ApplicationTask> findByApplicationIdOrderByCreatedAtDesc(UUID applicationId);

    boolean existsByIdAndApplicationId(UUID taskId, UUID applicationId);

    void updateTaskStatusAndFeedback(UUID taskId, ApplicationTaskStatus status, String hrFeedback);

    ApplicationTask addAttachment(UUID taskId, ApplicationTaskAttachment attachment);

    void deleteAttachment(UUID taskId, UUID attachmentId);

    void deleteTask(UUID taskId);
}
