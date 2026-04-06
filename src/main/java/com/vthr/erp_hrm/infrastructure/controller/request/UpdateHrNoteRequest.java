package com.vthr.erp_hrm.infrastructure.controller.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateHrNoteRequest {
    /** Ghi chú nội bộ HR; null hoặc rỗng sau trim = xóa ghi chú. */
    @Size(max = 8000, message = "HR note must be at most 8000 characters")
    private String hrNote;
}
