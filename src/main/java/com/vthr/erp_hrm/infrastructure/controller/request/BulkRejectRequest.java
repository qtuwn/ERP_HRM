package com.vthr.erp_hrm.infrastructure.controller.request;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class BulkRejectRequest {
    private List<UUID> applicationIds;
}
