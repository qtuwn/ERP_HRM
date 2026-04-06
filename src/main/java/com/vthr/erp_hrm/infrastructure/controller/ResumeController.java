package com.vthr.erp_hrm.infrastructure.controller;

import com.vthr.erp_hrm.core.model.Resume;
import com.vthr.erp_hrm.core.service.ResumeService;
import com.vthr.erp_hrm.infrastructure.controller.response.ApiResponse;
import com.vthr.erp_hrm.infrastructure.controller.response.ResumeResponse;
import com.vthr.erp_hrm.infrastructure.storage.SignedUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * Dùng {@link RequestParam} cho title/makeDefault (không dùng {@link RequestPart} với String):
     * một số client gửi part dạng {@code application/octet-stream} → Spring không bind được → 415/500.
     */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ResumeResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "makeDefault", required = false) Boolean makeDefault,
            Authentication authentication
    ) {
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

