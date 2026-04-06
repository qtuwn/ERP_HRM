package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.Application;
import com.vthr.erp_hrm.core.model.ApplicationStatus;
import com.vthr.erp_hrm.core.model.ApplicationTask;
import com.vthr.erp_hrm.core.model.ApplicationTaskAttachment;
import com.vthr.erp_hrm.core.model.ApplicationTaskDocumentType;
import com.vthr.erp_hrm.core.model.ApplicationTaskStatus;
import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.repository.ApplicationRepository;
import com.vthr.erp_hrm.core.repository.ApplicationTaskRepository;
import com.vthr.erp_hrm.core.service.ApplicationAccessService;
import com.vthr.erp_hrm.core.service.NotificationService;
import com.vthr.erp_hrm.infrastructure.storage.TaskDocumentStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationTaskServiceImplTest {

    @Mock
    private ApplicationTaskRepository taskRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private ApplicationAccessService applicationAccessService;
    @Mock
    private TaskDocumentStorageService taskDocumentStorageService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ApplicationTaskServiceImpl service;

    private final UUID appId = UUID.randomUUID();
    private final UUID taskId = UUID.randomUUID();
    private final UUID hrId = UUID.randomUUID();
    private final UUID candId = UUID.randomUUID();

    @Test
    void createTask_happyPath() {
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(
                Application.builder()
                        .id(appId)
                        .status(ApplicationStatus.HR_REVIEW)
                        .candidateId(candId)
                        .jobId(UUID.randomUUID())
                        .cvUrl("x")
                        .build()));
        when(taskRepository.createTask(any())).thenAnswer(inv -> {
            ApplicationTask t = inv.getArgument(0);
            return ApplicationTask.builder()
                    .id(taskId)
                    .applicationId(t.getApplicationId())
                    .title(t.getTitle())
                    .documentType(t.getDocumentType())
                    .status(ApplicationTaskStatus.OPEN)
                    .build();
        });

        ApplicationTask created = service.createTask(
                appId, hrId, Role.HR, "Nộp CCCD", "Mặt trước/sau", ApplicationTaskDocumentType.ID_CARD, null);

        org.assertj.core.api.Assertions.assertThat(created.getId()).isEqualTo(taskId);
        ArgumentCaptor<ApplicationTask> cap = ArgumentCaptor.forClass(ApplicationTask.class);
        verify(taskRepository).createTask(cap.capture());
        org.assertj.core.api.Assertions.assertThat(cap.getValue().getTitle()).isEqualTo("Nộp CCCD");
        verify(applicationAccessService).requireRecruiterForManagement(eq(hrId), eq(Role.HR), eq(appId));
    }

    @Test
    void createTask_rejectsWithdrawnApplication() {
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(
                Application.builder()
                        .id(appId)
                        .status(ApplicationStatus.WITHDRAWN)
                        .candidateId(candId)
                        .jobId(UUID.randomUUID())
                        .cvUrl("x")
                        .build()));

        assertThatThrownBy(() -> service.createTask(
                appId, hrId, Role.HR, "T", null, ApplicationTaskDocumentType.OTHER, null))
                .hasMessageContaining("withdrawn");
    }

    @Test
    void uploadAttachment_onlyCandidate() {
        assertThatThrownBy(() -> service.uploadAttachment(
                appId, taskId, hrId, Role.HR, new MockMultipartFile("f", "a.pdf", "application/pdf", new byte[]{1})))
                .hasMessageContaining("Only candidates");
    }

    @Test
    void uploadAttachment_rejectsWhenWithdrawn() {
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(
                Application.builder()
                        .id(appId)
                        .status(ApplicationStatus.WITHDRAWN)
                        .candidateId(candId)
                        .jobId(UUID.randomUUID())
                        .cvUrl("x")
                        .build()));

        assertThatThrownBy(() -> service.uploadAttachment(
                appId, taskId, candId, Role.CANDIDATE, new MockMultipartFile("f", "a.pdf", "application/pdf", new byte[]{1})))
                .hasMessageContaining("Cannot upload");
    }

    @Test
    void reviewTask_updatesRepository() {
        when(taskRepository.existsByIdAndApplicationId(taskId, appId)).thenReturn(true);
        when(taskRepository.findDetailById(taskId)).thenReturn(Optional.of(
                ApplicationTask.builder()
                        .id(taskId)
                        .applicationId(appId)
                        .status(ApplicationTaskStatus.APPROVED)
                        .attachments(List.of())
                        .build()));
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(
                Application.builder()
                        .id(appId)
                        .status(ApplicationStatus.HR_REVIEW)
                        .candidateId(candId)
                        .jobId(UUID.randomUUID())
                        .cvUrl("x")
                        .build()));

        service.reviewTask(appId, taskId, hrId, Role.HR, ApplicationTaskStatus.APPROVED, "OK");

        verify(taskRepository).updateTaskStatusAndFeedback(taskId, ApplicationTaskStatus.APPROVED, "OK");
        verify(applicationAccessService).requireRecruiterForManagement(eq(hrId), eq(Role.HR), eq(appId));
    }

    @Test
    void deleteAttachment_candidateMustOwnFile() {
        UUID attId = UUID.randomUUID();
        when(taskRepository.existsByIdAndApplicationId(taskId, appId)).thenReturn(true);
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(
                Application.builder()
                        .id(appId)
                        .status(ApplicationStatus.HR_REVIEW)
                        .candidateId(candId)
                        .jobId(UUID.randomUUID())
                        .cvUrl("x")
                        .build()));
        when(taskRepository.findDetailById(taskId)).thenReturn(Optional.of(
                ApplicationTask.builder()
                        .id(taskId)
                        .applicationId(appId)
                        .status(ApplicationTaskStatus.OPEN)
                        .attachments(List.of(
                                ApplicationTaskAttachment.builder()
                                        .id(attId)
                                        .uploadedByUserId(UUID.randomUUID())
                                        .build()))
                        .build()));

        assertThatThrownBy(() -> service.deleteAttachment(appId, taskId, attId, candId, Role.CANDIDATE))
                .hasMessageContaining("Access denied");
    }
}
