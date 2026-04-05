package com.vthr.erp_hrm.infrastructure.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailPayload {
    private UUID logId;
    private String recipient;
    private String subject;
    private String templateName;
    private Map<String, Object> variables;
}
