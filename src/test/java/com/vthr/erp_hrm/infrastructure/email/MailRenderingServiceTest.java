package com.vthr.erp_hrm.infrastructure.email;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailRenderingServiceTest {

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private MailRenderingService mailRenderingService;

    @Test
    void testRenderEmail_SuccessfulRender() {
        // Setup
        when(templateEngine.process(eq("email/welcome-template"), any(Context.class)))
                .thenReturn("<html>Welcome Email</html>");

        // Execute
        String result = mailRenderingService.renderEmail("welcome-template", new HashMap<>());

        // Verify
        assertEquals("<html>Welcome Email</html>", result);
        verify(templateEngine).process(eq("email/welcome-template"), any(Context.class));
    }

    @Test
    void testRenderEmail_WithVariables() {
        // Setup
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", "John");
        when(templateEngine.process(eq("email/welcome-template"), any(Context.class)))
                .thenReturn("<html>Welcome John</html>");

        // Execute
        String result = mailRenderingService.renderEmail("welcome-template", variables);

        // Verify
        assertEquals("<html>Welcome John</html>", result);
    }

    @Test
    void testRenderEmail_InvalidTemplateName_WithPathTraversal() {
        // Execute & Verify - should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, 
                () -> mailRenderingService.renderEmail("../../../etc/passwd", new HashMap<>()),
                "Path traversal attempt should be rejected");
    }

    @Test
    void testRenderEmail_InvalidTemplateName_WithSlash() {
        // Execute & Verify
        assertThrows(IllegalArgumentException.class,
                () -> mailRenderingService.renderEmail("folder/template", new HashMap<>()),
                "Template name with slash should be rejected");
    }

    @Test
    void testRenderEmail_InvalidTemplateName_WithSpecialChars() {
        // Execute & Verify
        assertThrows(IllegalArgumentException.class,
                () -> mailRenderingService.renderEmail("template@123!.txt", new HashMap<>()),
                "Template name with special chars should be rejected");
    }

    @Test
    void testRenderEmail_InvalidTemplateName_Null() {
        // Execute & Verify
        assertThrows(IllegalArgumentException.class,
                () -> mailRenderingService.renderEmail(null, new HashMap<>()),
                "Null template name should be rejected");
    }

    @Test
    void testRenderEmail_InvalidTemplateName_Empty() {
        // Execute & Verify
        assertThrows(IllegalArgumentException.class,
                () -> mailRenderingService.renderEmail("", new HashMap<>()),
                "Empty template name should be rejected");
    }

    @Test
    void testRenderEmail_ValidTemplateName_WithHyphens() {
        // Setup
        when(templateEngine.process(eq("email/welcome-2024-template"), any(Context.class)))
                .thenReturn("<html>Welcome</html>");

        // Execute
        String result = mailRenderingService.renderEmail("welcome-2024-template", new HashMap<>());

        // Verify
        assertEquals("<html>Welcome</html>", result);
    }

    @Test
    void testRenderEmail_ValidTemplateName_WithUnderscores() {
        // Setup
        when(templateEngine.process(eq("email/welcome_user_template"), any(Context.class)))
                .thenReturn("<html>Welcome</html>");

        // Execute
        String result = mailRenderingService.renderEmail("welcome_user_template", new HashMap<>());

        // Verify
        assertEquals("<html>Welcome</html>", result);
    }

    @Test
    void testGetRenderedTemplate_NoVariables() {
        // Setup
        when(templateEngine.process(eq("email/static-template"), any(Context.class)))
                .thenReturn("<html>Static Content</html>");

        // Execute
        String result = mailRenderingService.getRenderedTemplate("static-template");

        // Verify
        assertEquals("<html>Static Content</html>", result);
    }

    @Test
    void testGetRenderedTemplate_InvalidTemplateName() {
        // Execute & Verify
        assertThrows(IllegalArgumentException.class,
                () -> mailRenderingService.getRenderedTemplate("../malicious"),
                "Invalid template name should be rejected");
    }
}
