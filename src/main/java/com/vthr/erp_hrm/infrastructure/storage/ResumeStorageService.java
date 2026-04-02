package com.vthr.erp_hrm.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ResumeStorageService {
    String storeResume(MultipartFile file, UUID userId);
}

