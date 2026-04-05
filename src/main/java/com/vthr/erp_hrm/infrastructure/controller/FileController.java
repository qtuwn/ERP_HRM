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

/**
 * Controller xử lý việc tải xuống (download) file trong hệ thống.
 * Hỗ trợ hai loại file:
 * - CV ứng tuyển (gắn với jobId): /api/files/cvs/{jobId}/{filename}
 * - Resume cá nhân (gắn với userId): /api/files/resumes/{userId}/{filename}
 *
 * Tất cả các endpoint đều được bảo vệ bằng cơ chế signed URL
 * (URL có chữ ký và thời hạn), không yêu cầu đăng nhập JWT.
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final SignedUrlService signedUrlService;
    
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /**
     * Tải xuống file CV ứng tuyển theo jobId và tên file.
     * GET /api/files/cvs/{jobId}/{filename}
     *
     * Endpoint này không yêu cầu đăng nhập, nhưng yêu cầu signed URL hợp lệ
     * (tham số expires và signature) để ngăn chặn truy cập trái phép.
     *
     * @param jobId     UUID của tin tuyển dụng mà CV được nộp vào
     * @param filename  tên file CV cần tải xuống
     * @param expires   thời điểm hết hạn của URL (Unix timestamp, tính bằng giây)
     * @param signature chữ ký HMAC để xác thực tính hợp lệ của URL
     * @return file CV dưới dạng Resource nếu hợp lệ;
     *         403 nếu chữ ký sai hoặc hết hạn;
     *         404 nếu file không tồn tại;
     *         500 nếu có lỗi đọc file
     */
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

    /**
     * Tải xuống file Resume (CV cá nhân) theo userId và tên file.
     * GET /api/files/resumes/{userId}/{filename}
     *
     * Tương tự downloadCv nhưng file được lưu trong thư mục con "resumes"
     * và được tổ chức theo userId thay vì jobId.
     *
     * @param userId    UUID của ứng viên sở hữu resume
     * @param filename  tên file resume cần tải xuống
     * @param expires   thời điểm hết hạn của URL (Unix timestamp, tính bằng giây)
     * @param signature chữ ký HMAC để xác thực tính hợp lệ của URL
     * @return file resume dưới dạng Resource nếu hợp lệ;
     *         403 nếu chữ ký sai hoặc hết hạn;
     *         404 nếu file không tồn tại;
     *         500 nếu có lỗi đọc file
     */
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
}
