package com.vthr.erp_hrm.infrastructure.storage;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class LocalResumeStorageService implements ResumeStorageService {

    private final Path rootLocation;

    public LocalResumeStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("resumes");
    }

    @PostConstruct
    void ensureRootExists() {
        try {
            Files.createDirectories(rootLocation);
            log.info("Resume storage root ready at {}", rootLocation);
        } catch (IOException e) {
            log.error("Cannot create resume storage directory at {}", rootLocation, e);
            throw new RuntimeException(
                    "Cannot create resume storage directory. Check file.upload-dir and filesystem permissions.", e);
        }
    }

    @Override
    public String storeResume(MultipartFile file, UUID userId) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is empty.");
        }

        String rawName = file.getOriginalFilename();
        if (rawName == null || rawName.isBlank()) {
            rawName = "document.pdf";
        }
        String originalFileName = StringUtils.cleanPath(rawName);

        if (originalFileName.contains("..")) {
            throw new RuntimeException("Filename contains invalid path sequence: " + originalFileName);
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("File size exceeds 5MB limit.");
        }

        String lower = originalFileName.toLowerCase();
        boolean okExt = lower.endsWith(".pdf") || lower.endsWith(".docx");
        String contentType = file.getContentType();
        boolean okMime = contentType != null
                && ("application/pdf".equals(contentType)
                        || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType)
                        || "application/x-pdf".equals(contentType));
        // Trình duyệt đôi khi gửi application/octet-stream hoặc MIME rỗng — cho phép nếu đuôi file hợp lệ
        if (!okExt && !okMime) {
            throw new RuntimeException("Invalid file type. Only PDF and DOCX are allowed.");
        }
        if (!okMime && okExt) {
            log.debug("Accepting resume by extension (MIME was: {})", contentType);
        }

        String extension = "";
        int i = originalFileName.lastIndexOf('.');
        if (i >= 0) {
            extension = originalFileName.substring(i);
        } else if (okMime && "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType)) {
            extension = ".docx";
        } else if (okMime && ("application/pdf".equals(contentType) || "application/x-pdf".equals(contentType))) {
            extension = ".pdf";
        }

        String targetFileName = UUID.randomUUID().toString() + extension;
        Path userDir = this.rootLocation.resolve(userId.toString());
        try {
            Files.createDirectories(userDir);
            Path targetLocation = userDir.resolve(targetFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to write resume file for user {} under {}", userId, rootLocation, e);
            throw new RuntimeException(
                    "Could not save resume file. Check disk space and write permissions for: " + rootLocation, e);
        }

        // objectPath = "{userId}/{filename}" - used by SignedUrlService & FileController
        return userId.toString() + "/" + targetFileName;
    }
}

