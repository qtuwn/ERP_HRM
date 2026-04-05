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

/**
 * Controller quản lý CV (Resume) của ứng viên (CANDIDATE).
 * Tất cả các endpoint đều yêu cầu người dùng có role CANDIDATE
 * và đã đăng nhập (xác thực qua JWT).
 * Base URL: /api/users/me/resumes
 */
@RestController
@RequestMapping("/api/users/me/resumes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDATE')")
public class ResumeController {

    // Service xử lý logic nghiệp vụ liên quan đến CV
    private final ResumeService resumeService;

    // Service tạo signed URL (URL có chữ ký) để truy cập file CV an toàn
    private final SignedUrlService signedUrlService;

    /**
     * Lấy danh sách tất cả CV của ứng viên đang đăng nhập.
     * GET /api/users/me/resumes
     *
     * @param authentication thông tin xác thực của người dùng hiện tại (chứa userId)
     * @return danh sách CV kèm signed URL để tải file
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ResumeResponse>>> list(Authentication authentication) {
        // Lấy userId từ thông tin xác thực (authentication)
        UUID userId = UUID.fromString(authentication.getName());
        // Truy vấn danh sách CV, chuyển đổi sang ResumeResponse kèm signed URL
        List<ResumeResponse> data = resumeService.listMyResumes(userId).stream()
                .map(r -> ResumeResponse.fromDomain(r, sign(r)))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(data, "OK"));
    }

    /**
     * Upload CV mới cho ứng viên đang đăng nhập.
     * POST /api/users/me/resumes
     * Content-Type: multipart/form-data
     *
     * @param file        file CV cần upload (bắt buộc)
     * @param title       tiêu đề đặt cho CV, tối đa 120 ký tự (tùy chọn)
     * @param makeDefault đánh dấu CV này là CV mặc định hay không (tùy chọn)
     * @param authentication thông tin xác thực của người dùng hiện tại
     * @return thông tin CV vừa upload kèm signed URL
     */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ResumeResponse>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "title", required = false) @Size(max = 120) String title,
            @RequestPart(value = "makeDefault", required = false) Boolean makeDefault,
            Authentication authentication
    ) throws IOException {
        // Lấy userId từ thông tin xác thực
        UUID userId = UUID.fromString(authentication.getName());
        // Gọi service upload file và lưu thông tin CV vào database
        Resume r = resumeService.uploadMyResume(userId, file, title, makeDefault);
        return ResponseEntity.ok(ApiResponse.success(ResumeResponse.fromDomain(r, sign(r)), "Uploaded"));
    }

    /**
     * Đặt một CV cụ thể làm CV mặc định.
     * POST /api/users/me/resumes/{id}/default
     *
     * @param id             UUID của CV cần đặt làm mặc định
     * @param authentication thông tin xác thực của người dùng hiện tại
     * @return thông tin CV vừa được đặt làm mặc định
     */
    @PostMapping("/{id}/default")
    public ResponseEntity<ApiResponse<ResumeResponse>> setDefault(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        // Lấy userId từ thông tin xác thực
        UUID userId = UUID.fromString(authentication.getName());
        // Gọi service cập nhật CV mặc định
        Resume r = resumeService.setDefaultResume(userId, id);
        return ResponseEntity.ok(ApiResponse.success(ResumeResponse.fromDomain(r, sign(r)), "OK"));
    }

    /**
     * Xóa một CV của ứng viên đang đăng nhập.
     * DELETE /api/users/me/resumes/{id}
     *
     * @param id             UUID của CV cần xóa
     * @param authentication thông tin xác thực của người dùng hiện tại
     * @return response thành công không kèm dữ liệu
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        // Lấy userId từ thông tin xác thực
        UUID userId = UUID.fromString(authentication.getName());
        // Gọi service xóa CV (xóa cả file trên storage và bản ghi trong database)
        resumeService.deleteMyResume(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted"));
    }

    /**
     * Tạo signed URL (URL có chữ ký, có thời hạn) để truy cập file CV an toàn.
     * Trả về null nếu CV không có đường dẫn file (storagePath).
     *
     * @param r đối tượng Resume cần tạo signed URL
     * @return signed URL dạng chuỗi, hoặc null nếu không có file
     */
    private String sign(Resume r) {
        // Kiểm tra null để tránh lỗi khi CV không có file đính kèm
        if (r == null || r.getStoragePath() == null) {
            return null;
        }
         // Tạo signed URL từ đường dẫn lưu trữ của file CV
        return signedUrlService.generateSignedUrl("/api/files/resumes", r.getStoragePath());
    }
}

