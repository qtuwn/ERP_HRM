package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.Application;
import com.vthr.erp_hrm.core.model.ApplicationStatus;
import com.vthr.erp_hrm.core.model.ApplicationTask;
import com.vthr.erp_hrm.core.model.ApplicationTaskAttachment;
import com.vthr.erp_hrm.core.model.ApplicationTaskDocumentType;
import com.vthr.erp_hrm.core.model.ApplicationTaskStatus;
import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.model.NotificationType;
import com.vthr.erp_hrm.core.repository.ApplicationRepository;
import com.vthr.erp_hrm.core.repository.ApplicationTaskRepository;
import com.vthr.erp_hrm.core.service.ApplicationAccessService;
import com.vthr.erp_hrm.core.service.ApplicationTaskService;
import com.vthr.erp_hrm.core.service.NotificationService;
import com.vthr.erp_hrm.infrastructure.storage.TaskDocumentStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationTaskServiceImpl implements ApplicationTaskService {

    private final ApplicationTaskRepository taskRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationAccessService applicationAccessService;
    private final TaskDocumentStorageService taskDocumentStorageService;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationTask> listTasks(UUID applicationId, UUID userId, Role role) {
        applicationAccessService.requireParticipantForMessaging(userId, role, applicationId);
        return taskRepository.findByApplicationIdOrderByCreatedAtDesc(applicationId);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationTask getTask(UUID applicationId, UUID taskId, UUID userId, Role role) {
        applicationAccessService.requireParticipantForMessaging(userId, role, applicationId);
        if (!taskRepository.existsByIdAndApplicationId(taskId, applicationId)) {
            throw new RuntimeException("Task not found");
        }
        return taskRepository.findDetailById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
    }

    @Override
    @Transactional
    public ApplicationTask createTask(
            UUID applicationId,
            UUID recruiterUserId,
            Role role,
            String title,
            String description,
            ApplicationTaskDocumentType documentType,
            ZonedDateTime dueAt
    ) {
        applicationAccessService.requireRecruiterForManagement(recruiterUserId, role, applicationId);
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        if (app.getStatus() == ApplicationStatus.WITHDRAWN) {
            throw new RuntimeException("Cannot assign tasks to a withdrawn application");
        }
        ApplicationTask task = ApplicationTask.builder()
                .applicationId(applicationId)
                .title(title != null ? title.trim() : "")
                .description(description)
                .documentType(documentType != null ? documentType : ApplicationTaskDocumentType.OTHER)
                .dueAt(dueAt)
                .createdByUserId(recruiterUserId)
                .build();
        if (task.getTitle().isEmpty()) {
            throw new RuntimeException("Title is required");
        }
        ApplicationTask created = taskRepository.createTask(task);
        try {
            notificationService.create(
                    app.getCandidateId(),
                    NotificationType.APPLICATION_TASK_ASSIGNED,
                    "HR giao nhiệm vụ mới",
                    created.getTitle(),
                    "/candidate/applications/" + applicationId + "/tasks",
                    null
            );
        } catch (Exception ignored) {
            // ignore
        }
        return created;
    }

    @Override
    @Transactional
    public ApplicationTask uploadAttachment(
            UUID applicationId,
            UUID taskId,
            UUID userId,
            Role role,
            MultipartFile file
    ) throws IOException {
        if (role != Role.CANDIDATE) {
            throw new RuntimeException("Only candidates can upload task documents");
        }
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        if (!app.getCandidateId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }
        if (app.getStatus() == ApplicationStatus.WITHDRAWN || app.getStatus() == ApplicationStatus.REJECTED) {
            throw new RuntimeException("Cannot upload documents for this application");
        }
        if (!taskRepository.existsByIdAndApplicationId(taskId, applicationId)) {
            throw new RuntimeException("Task not found");
        }
        ApplicationTask detail = taskRepository.findDetailById(taskId).orElseThrow();
        if (detail.getStatus() == ApplicationTaskStatus.APPROVED) {
            throw new RuntimeException("Task already approved");
        }
        String storagePath = taskDocumentStorageService.store(file, applicationId, taskId);
        ApplicationTaskAttachment att = ApplicationTaskAttachment.builder()
                .storagePath(storagePath)
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedByUserId(userId)
                .build();
        ApplicationTask after = taskRepository.addAttachment(taskId, att);
        // Candidate upload xong: có thể mở rộng notify HR sau; hiện giữ scope candidate.
        try {
            notificationService.create(
                    userId,
                    NotificationType.APPLICATION_TASK_DOCUMENT_UPLOADED,
                    "Đã tải lên tài liệu",
                    detail.getTitle(),
                    "/candidate/applications/" + applicationId + "/tasks",
                    null
            );
        } catch (Exception ignored) {
            // ignore
        }
        return after;
    }

    @Override
    @Transactional
    public ApplicationTask reviewTask(
            UUID applicationId,
            UUID taskId,
            UUID recruiterUserId,
            Role role,
            ApplicationTaskStatus status,
            String hrFeedback
    ) {
        applicationAccessService.requireRecruiterForManagement(recruiterUserId, role, applicationId);
        if (!taskRepository.existsByIdAndApplicationId(taskId, applicationId)) {
            throw new RuntimeException("Task not found");
        }
        if (status != ApplicationTaskStatus.APPROVED && status != ApplicationTaskStatus.REJECTED) {
            throw new RuntimeException("Invalid review status");
        }
        taskRepository.updateTaskStatusAndFeedback(taskId, status, hrFeedback);
        ApplicationTask reviewed = taskRepository.findDetailById(taskId).orElseThrow();
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        try {
            notificationService.create(
                    app.getCandidateId(),
                    NotificationType.APPLICATION_TASK_REVIEWED,
                    "HR đã phản hồi tài liệu",
                    reviewed.getTitle(),
                    "/candidate/applications/" + applicationId + "/tasks",
                    null
            );
        } catch (Exception ignored) {
            // ignore
        }
        return reviewed;
    }

    @Override
    @Transactional
    public void deleteTask(UUID applicationId, UUID taskId, UUID recruiterUserId, Role role) {
        applicationAccessService.requireRecruiterForManagement(recruiterUserId, role, applicationId);
        if (!taskRepository.existsByIdAndApplicationId(taskId, applicationId)) {
            throw new RuntimeException("Task not found");
        }
        taskRepository.deleteTask(taskId);
    }

    @Override
    @Transactional
    public void deleteAttachment(UUID applicationId, UUID taskId, UUID attachmentId, UUID userId, Role role) {
        if (!taskRepository.existsByIdAndApplicationId(taskId, applicationId)) {
            throw new RuntimeException("Task not found");
        }
        ApplicationTask detail = taskRepository.findDetailById(taskId).orElseThrow();
        detail.getAttachments().stream()
                .filter(a -> a.getId().equals(attachmentId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Attachment not found"));

        if (role == Role.CANDIDATE) {
            Application app = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new RuntimeException("Application not found"));
            if (!app.getCandidateId().equals(userId)) {
                throw new RuntimeException("Access denied");
            }
            boolean owns = detail.getAttachments().stream()
                    .anyMatch(a -> a.getId().equals(attachmentId) && a.getUploadedByUserId().equals(userId));
            if (!owns) {
                throw new RuntimeException("Access denied");
            }
            if (detail.getStatus() == ApplicationTaskStatus.APPROVED) {
                throw new RuntimeException("Cannot remove attachment from approved task");
            }
        } else if (role == Role.HR || role == Role.COMPANY || role == Role.ADMIN) {
            applicationAccessService.requireRecruiterForManagement(userId, role, applicationId);
        } else {
            throw new RuntimeException("Access denied");
        }

        taskRepository.deleteAttachment(taskId, attachmentId);
        if (role == Role.CANDIDATE) {
            ApplicationTask after = taskRepository.findDetailById(taskId).orElseThrow();
            if (after.getAttachments().isEmpty()) {
                taskRepository.updateTaskStatusAndFeedback(taskId, ApplicationTaskStatus.OPEN, null);
            }
        }
    }

}
