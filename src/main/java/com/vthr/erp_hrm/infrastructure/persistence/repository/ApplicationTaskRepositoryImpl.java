package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.core.model.ApplicationTask;
import com.vthr.erp_hrm.core.model.ApplicationTaskAttachment;
import com.vthr.erp_hrm.core.model.ApplicationTaskDocumentType;
import com.vthr.erp_hrm.core.model.ApplicationTaskStatus;
import com.vthr.erp_hrm.core.repository.ApplicationTaskRepository;
import com.vthr.erp_hrm.infrastructure.persistence.entity.ApplicationTaskAttachmentEntity;
import com.vthr.erp_hrm.infrastructure.persistence.entity.ApplicationTaskEntity;
import com.vthr.erp_hrm.infrastructure.persistence.mapper.ApplicationTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ApplicationTaskRepositoryImpl implements ApplicationTaskRepository {

    private final ApplicationTaskJpaRepository jpaRepository;

    @Override
    @Transactional
    public ApplicationTask createTask(ApplicationTask task) {
        ApplicationTaskEntity e = new ApplicationTaskEntity();
        e.setApplicationId(task.getApplicationId());
        e.setTitle(task.getTitle());
        e.setDescription(task.getDescription());
        e.setDocumentType(task.getDocumentType() != null ? task.getDocumentType().name() : ApplicationTaskDocumentType.OTHER.name());
        e.setStatus(ApplicationTaskStatus.OPEN.name());
        e.setDueAt(task.getDueAt());
        e.setCreatedByUserId(task.getCreatedByUserId());
        ApplicationTaskEntity saved = jpaRepository.save(e);
        return ApplicationTaskMapper.toDomain(saved, false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ApplicationTask> findDetailById(UUID id) {
        return jpaRepository.findDetailById(id).map(entity -> ApplicationTaskMapper.toDomain(entity, true));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationTask> findByApplicationIdOrderByCreatedAtDesc(UUID applicationId) {
        return jpaRepository.findByApplicationIdOrderByCreatedAtDesc(applicationId).stream()
                .map(e -> ApplicationTaskMapper.toDomain(e, false))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByIdAndApplicationId(UUID taskId, UUID applicationId) {
        return jpaRepository.existsByIdAndApplicationId(taskId, applicationId);
    }

    @Override
    @Transactional
    public void updateTaskStatusAndFeedback(UUID taskId, ApplicationTaskStatus status, String hrFeedback) {
        ApplicationTaskEntity e = jpaRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        e.setStatus(status.name());
        e.setHrFeedback(hrFeedback);
        jpaRepository.save(e);
    }

    @Override
    @Transactional
    public ApplicationTask addAttachment(UUID taskId, ApplicationTaskAttachment attachment) {
        ApplicationTaskEntity task = jpaRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        ApplicationTaskAttachmentEntity a = new ApplicationTaskAttachmentEntity();
        a.setTask(task);
        a.setStoragePath(attachment.getStoragePath());
        a.setOriginalFilename(attachment.getOriginalFilename());
        a.setContentType(attachment.getContentType());
        a.setFileSize(attachment.getFileSize());
        a.setUploadedByUserId(attachment.getUploadedByUserId());
        task.getAttachments().add(a);
        task.setStatus(ApplicationTaskStatus.SUBMITTED.name());
        task.setHrFeedback(null);
        jpaRepository.save(task);
        return jpaRepository.findDetailById(taskId)
                .map(entity -> ApplicationTaskMapper.toDomain(entity, true))
                .orElseThrow();
    }

    @Override
    @Transactional
    public void deleteAttachment(UUID taskId, UUID attachmentId) {
        ApplicationTaskEntity task = jpaRepository.findDetailById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        task.getAttachments().removeIf(att -> att.getId().equals(attachmentId));
        jpaRepository.save(task);
    }

    @Override
    @Transactional
    public void deleteTask(UUID taskId) {
        jpaRepository.deleteById(taskId);
    }
}
