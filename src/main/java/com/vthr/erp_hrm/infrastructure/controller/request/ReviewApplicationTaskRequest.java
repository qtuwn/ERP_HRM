package com.vthr.erp_hrm.infrastructure.controller.request;

import com.vthr.erp_hrm.core.model.ApplicationTaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewApplicationTaskRequest {
    @NotNull
    private ApplicationTaskStatus status;
    private String hrFeedback;
}
