package com.vthr.erp_hrm.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

public interface FileStorageService {
    String storeFile(MultipartFile file, UUID jobId) throws IOException;

    String copyResumeToJobCv(String resumeStoragePath, UUID jobId) throws IOException;
}
