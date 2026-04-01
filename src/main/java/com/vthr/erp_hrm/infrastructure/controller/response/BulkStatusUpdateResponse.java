package com.vthr.erp_hrm.infrastructure.controller.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class BulkStatusUpdateResponse {
    private List<UUID> succeededIds;
    private Map<UUID, String> failed;
}

