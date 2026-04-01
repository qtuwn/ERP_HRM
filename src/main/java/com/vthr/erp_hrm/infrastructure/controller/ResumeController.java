package com.vthr.erp_hrm.infrastructure.controller;

import com.vthr.erp_hrm.core.model.Resume;
import com.vthr.erp_hrm.core.service.ResumeService;
import com.vthr.erp_hrm.infrastructure.controller.response.ApiResponse;
import com.vthr.erp_hrm.infrastructure.controller.response.ResumeResponse;
import com.vthr.erp_hrm.infrastructure.storage.SignedUrlService;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/me/resumes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDATE')")
public class ResumeController {

    private final ResumeService resumeService;
    private final SignedUrlService signedUrlService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ResumeResponse>>> list(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        List<ResumeResponse> data = resumeService.listMyResumes(userId).stream()
                .map(r -> ResumeResponse.fromDomain(r, sign(r)))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(data, "OK"));
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ResumeResponse>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "title", required = false) @Size(max = 120) String title,
            @RequestPart(value = "makeDefault", required = false) Boolean makeDefault,
            Authentication authentication
    ) throws IOException {
        UUID userId = UUID.fromString(authentication.getName());
        Resume r = resumeService.uploadMyResume(userId, file, title, makeDefault);
        return ResponseEntity.ok(ApiResponse.success(ResumeResponse.fromDomain(r, sign(r)), "Uploaded"));
    }

    @PostMapping("/{id}/default")
    public ResponseEntity<ApiResponse<ResumeResponse>> setDefault(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        Resume r = resumeService.setDefaultResume(userId, id);
        return ResponseEntity.ok(ApiResponse.success(ResumeResponse.fromDomain(r, sign(r)), "OK"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        resumeService.deleteMyResume(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted"));
    }

    private String sign(Resume r) {
        if (r == null || r.getStoragePath() == null) {
            return null;
        }
        return signedUrlService.generateSignedUrl("/api/files/resumes", r.getStoragePath());
    }
}

