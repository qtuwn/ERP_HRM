package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.Resume;
import com.vthr.erp_hrm.core.repository.ResumeRepository;
import com.vthr.erp_hrm.core.repository.UserRepository;
import com.vthr.erp_hrm.core.service.ResumeService;
import com.vthr.erp_hrm.infrastructure.storage.ResumeStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeStorageService resumeStorageService;
    private final UserRepository userRepository;

    @Override
    public List<Resume> listMyResumes(UUID userId) {
        requireUserExists(userId);
        return resumeRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional
    public Resume uploadMyResume(UUID userId, MultipartFile file, String title, Boolean makeDefault) {
        requireUserExists(userId);
        String normalizedTitle = (title == null || title.isBlank()) ? "CV" : title.trim();
        if (normalizedTitle.length() > 120) {
            throw new RuntimeException("Title is too long");
        }

        String objectPath = resumeStorageService.storeResume(file, userId);

        boolean shouldMakeDefault = Boolean.TRUE.equals(makeDefault) || resumeRepository.countByUserId(userId) == 0;
        if (shouldMakeDefault) {
            resumeRepository.unsetDefaultForUser(userId);
        }

        Resume resume = Resume.builder()
                .userId(userId)
                .title(normalizedTitle)
                .storagePath(objectPath)
                .originalFilename(file.getOriginalFilename())
                .mimeType(file.getContentType())
                .sizeBytes(file.getSize())
                .isDefault(shouldMakeDefault)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();
        return resumeRepository.save(resume);
    }

    @Override
    @Transactional
    public Resume setDefaultResume(UUID userId, UUID resumeId) {
        requireUserExists(userId);
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));
        if (!userId.equals(resume.getUserId())) {
            throw new RuntimeException("Access denied");
        }
        resumeRepository.unsetDefaultForUser(userId);
        resume.setDefault(true);
        resume.setUpdatedAt(ZonedDateTime.now());
        return resumeRepository.save(resume);
    }

    @Override
    public void deleteMyResume(UUID userId, UUID resumeId) {
        requireUserExists(userId);
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));
        if (!userId.equals(resume.getUserId())) {
            throw new RuntimeException("Access denied");
        }
        resumeRepository.deleteById(resumeId);
    }

    @Override
    public Resume getMyResume(UUID userId, UUID resumeId) {
        requireUserExists(userId);
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));
        if (!userId.equals(resume.getUserId())) {
            throw new RuntimeException("Access denied");
        }
        return resume;
    }

    private void requireUserExists(UUID userId) {
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
    }
}

