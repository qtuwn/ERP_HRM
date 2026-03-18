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
}
