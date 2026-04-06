package com.vthr.erp_hrm.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Lưu tài liệu nhiệm vụ (CCCD, bằng cấp, …) dưới {@code uploads/task-docs/...}.
 */
public interface TaskDocumentStorageService {

    /**
     * @return đường dẫn tương đối: {@code applicationId/taskId/uuid.ext} (không có prefix task-docs)
     */
    String store(MultipartFile file, UUID applicationId, UUID taskId) throws IOException;
}
