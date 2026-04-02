package com.vthr.erp_hrm.core.service;

import com.vthr.erp_hrm.core.model.Resume;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ResumeService {
    List<Resume> listMyResumes(UUID userId);
    Resume uploadMyResume(UUID userId, MultipartFile file, String title, Boolean makeDefault);
    Resume setDefaultResume(UUID userId, UUID resumeId);
    void deleteMyResume(UUID userId, UUID resumeId);

    Resume getMyResume(UUID userId, UUID resumeId);
}

