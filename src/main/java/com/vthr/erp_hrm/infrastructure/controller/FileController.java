package com.vthr.erp_hrm.infrastructure.controller;

import com.vthr.erp_hrm.infrastructure.storage.SignedUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final SignedUrlService signedUrlService;
    
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @GetMapping("/cvs/{jobId}/{filename}")
    public ResponseEntity<Resource> downloadCv(
            @PathVariable UUID jobId,
            @PathVariable String filename,
            @RequestParam long expires,
            @RequestParam String signature) {

        String objectPath = jobId.toString() + "/" + filename;
        
        if (!signedUrlService.verifySignature(objectPath, expires, signature)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Path file = Paths.get(uploadDir).resolve(objectPath).normalize();
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                String contentType = filename.endsWith(".pdf") ? "application/pdf" : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/resumes/{userId}/{filename}")
    public ResponseEntity<Resource> downloadResume(
            @PathVariable UUID userId,
            @PathVariable String filename,
            @RequestParam long expires,
            @RequestParam String signature
    ) {
        String objectPath = userId.toString() + "/" + filename;

        if (!signedUrlService.verifySignature(objectPath, expires, signature)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Path file = Paths.get(uploadDir).resolve("resumes").resolve(objectPath).normalize();
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                String contentType = filename.endsWith(".pdf")
                        ? "application/pdf"
                        : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/task-docs/{applicationId}/{taskId}/{filename}")
    public ResponseEntity<Resource> downloadTaskDocument(
            @PathVariable UUID applicationId,
            @PathVariable UUID taskId,
            @PathVariable String filename,
            @RequestParam long expires,
            @RequestParam String signature) {

        String objectPath = applicationId + "/" + taskId + "/" + filename;
        if (!signedUrlService.verifySignature(objectPath, expires, signature)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (filename.contains("..")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Path file = Paths.get(uploadDir).resolve("task-docs").resolve(objectPath).normalize();
            Path root = Paths.get(uploadDir).resolve("task-docs").normalize();
            if (!file.startsWith(root)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            String lower = filename.toLowerCase(Locale.ROOT);
            String contentType = "application/octet-stream";
            if (lower.endsWith(".pdf")) {
                contentType = "application/pdf";
            } else if (lower.endsWith(".docx")) {
                contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            } else if (lower.endsWith(".png")) {
                contentType = "image/png";
            } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
