package com.vthr.erp_hrm.infrastructure.storage;

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
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path rootLocation;

    public LocalFileStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String storeFile(MultipartFile file, UUID jobId) throws IOException {
        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equals("application/pdf") || 
            contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))) {
            throw new RuntimeException("Invalid file type. Only PDF and DOCX are allowed.");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("File size exceeds 5MB limit.");
        }

        if (originalFileName.contains("..")) {
            throw new RuntimeException("Filename contains invalid path sequence: " + originalFileName);
        }

        String extension = "";
        int i = originalFileName.lastIndexOf('.');
        if (i >= 0) {
            extension = originalFileName.substring(i);
        }

        String targetFileName = UUID.randomUUID().toString() + extension;
        Path jobDir = this.rootLocation.resolve(jobId.toString());
        Files.createDirectories(jobDir);
        
        Path targetLocation = jobDir.resolve(targetFileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        return jobId.toString() + "/" + targetFileName;
    }

    @Override
    public String copyResumeToJobCv(String resumeStoragePath, UUID jobId) throws IOException {
        if (resumeStoragePath == null || resumeStoragePath.isBlank()) {
            throw new RuntimeException("Resume storage path is blank");
        }

        String normalized = resumeStoragePath.trim().replace("\\", "/");
        if (normalized.contains("..")) {
            throw new RuntimeException("Invalid resume path");
        }

        Path source = this.rootLocation.resolve("resumes").resolve(normalized).normalize();
        if (!source.startsWith(this.rootLocation.resolve("resumes").normalize())) {
            throw new RuntimeException("Invalid resume path");
        }
        if (!Files.exists(source)) {
            throw new RuntimeException("Resume file not found");
        }

        String filename = source.getFileName().toString();
        String extension = "";
        int i = filename.lastIndexOf('.');
        if (i >= 0) {
            extension = filename.substring(i);
        }

        String targetFileName = UUID.randomUUID().toString() + extension;
        Path jobDir = this.rootLocation.resolve(jobId.toString());
        Files.createDirectories(jobDir);

        Path target = jobDir.resolve(targetFileName);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

        return jobId.toString() + "/" + targetFileName;
    }
}
