package com.vthr.erp_hrm.infrastructure.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailRenderingService {

    private final TemplateEngine templateEngine;
    
    // Validate template name to prevent path traversal and directory traversal attacks
    private static final String TEMPLATE_NAME_PATTERN = "^[a-zA-Z0-9_-]+$";

    public String renderEmail(String templateName, Map<String, Object> variables) {
        validateTemplateName(templateName);
        
        Context context = new Context();
        if (variables != null) {
            context.setVariables(variables);
        }
        
        String templatePath = "email/" + templateName;
        return templateEngine.process(templatePath, context);
    }

    @Cacheable(value = "emailTemplates", key = "#templateName", unless = "#result == null")
    public String getRenderedTemplate(String templateName) {
        validateTemplateName(templateName);
        String templatePath = "email/" + templateName;
        return templateEngine.process(templatePath, new Context());
    }

    private void validateTemplateName(String templateName) {
        if (templateName == null || templateName.isEmpty()) {
            throw new IllegalArgumentException("Template name cannot be null or empty");
        }
        if (!templateName.matches(TEMPLATE_NAME_PATTERN)) {
            throw new IllegalArgumentException("Invalid template name: " + templateName + ". Only alphanumeric, hyphens, and underscores allowed");
        }
        if (templateName.contains("..") || templateName.contains("/") || templateName.contains("\\")) {
            throw new IllegalArgumentException("Template name contains invalid path components");
        }
    }
}

