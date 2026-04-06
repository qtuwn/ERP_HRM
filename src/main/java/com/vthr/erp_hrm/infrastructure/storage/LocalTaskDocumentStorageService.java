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
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class LocalTaskDocumentStorageService implements TaskDocumentStorageService {

    private static final long MAX_BYTES = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/png",
            "image/jpeg",
            "image/jpg"
    );

    private final Path taskDocsRoot;

    public LocalTaskDocumentStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.taskDocsRoot = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("task-docs");
    }

    @Override
    public String store(MultipartFile file, UUID applicationId, UUID taskId) throws IOException {
        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new RuntimeException("Chỉ chấp nhận PDF, DOCX, PNG, JPEG.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new RuntimeException("File vượt quá 10MB.");
        }
        if (originalFileName.contains("..")) {
            throw new RuntimeException("Tên file không hợp lệ.");
        }
        String extension = "";
        int i = originalFileName.lastIndexOf('.');
        if (i >= 0) {
            extension = originalFileName.substring(i);
        }
        String targetFileName = UUID.randomUUID() + extension;
        Path dir = taskDocsRoot.resolve(applicationId.toString()).resolve(taskId.toString());
        Files.createDirectories(dir);
        Path target = dir.resolve(targetFileName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return applicationId + "/" + taskId + "/" + targetFileName;
    }
}
