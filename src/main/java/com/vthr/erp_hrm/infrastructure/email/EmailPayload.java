package com.vthr.erp_hrm.infrastructure.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    
    @NotNull(message = "Log ID cannot be null")
    private UUID logId;
    
    @NotBlank(message = "Recipient email cannot be blank")
    @Email(message = "Invalid email format")
    private String recipient;
    
    @NotBlank(message = "Subject cannot be blank")
    private String subject;
    
    @NotBlank(message = "Template name cannot be blank")
    private String templateName;
    
    private Map<String, Object> variables;

    @Override
    public String toString() {
        // Safe toString that doesn't expose sensitive data like recipient details in logs
        return "EmailPayload{" +
                "logId=" + logId +
                ", recipient_masked=" + maskEmail(recipient) +
                ", templateName='" + templateName + '\'' +
                '}';
    }

    private static String maskEmail(String email) {
        if (email == null || email.length() < 2) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***";
        }
        return email.charAt(0) + "***@" + email.substring(atIndex + 1);
    }
}

