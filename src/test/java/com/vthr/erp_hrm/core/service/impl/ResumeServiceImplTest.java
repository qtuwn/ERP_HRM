package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.Resume;
import com.vthr.erp_hrm.core.model.User;
import com.vthr.erp_hrm.core.repository.ResumeRepository;
import com.vthr.erp_hrm.core.repository.UserRepository;
import com.vthr.erp_hrm.infrastructure.storage.ResumeStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeServiceImplTest {

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private ResumeStorageService resumeStorageService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ResumeServiceImpl resumeService;

    @Test
    void uploadMyResume_firstResumeShouldBecomeDefault() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder().id(userId).build()));
        when(resumeRepository.countByUserId(userId)).thenReturn(0L);
        doNothing().when(resumeRepository).unsetDefaultForUser(userId);
        when(resumeStorageService.storeResume(any(), eq(userId))).thenReturn(userId + "/x.pdf");
        when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "data".getBytes());
        Resume saved = resumeService.uploadMyResume(userId, file, "My CV", null);

        assertTrue(saved.isDefault());
        verify(resumeRepository).unsetDefaultForUser(userId);
    }
}

