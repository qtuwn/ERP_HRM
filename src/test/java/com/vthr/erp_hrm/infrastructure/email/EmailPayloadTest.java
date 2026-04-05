package com.vthr.erp_hrm.infrastructure.email;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EmailPayloadTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidEmailPayload() {
        // Create valid payload
        EmailPayload payload = EmailPayload.builder()
                .logId(UUID.randomUUID())
                .recipient("user@example.com")
                .subject("Welcome")
                .templateName("welcome-template")
                .variables(new HashMap<>())
                .build();

        // Validate
        Set<ConstraintViolation<EmailPayload>> violations = validator.validate(payload);
        assertTrue(violations.isEmpty(), "Valid payload should have no violations");
    }

    @Test
    void testMissingLogId() {
        // Create payload without logId
        EmailPayload payload = EmailPayload.builder()
                .recipient("user@example.com")
                .subject("Welcome")
                .templateName("welcome-template")
                .build();

        // Validate
        Set<ConstraintViolation<EmailPayload>> violations = validator.validate(payload);
        assertEquals(1, violations.size(), "Missing logId should cause violation");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("logId")));
    }

    @Test
    void testInvalidEmailFormat() {
        // Create payload with invalid email
        EmailPayload payload = EmailPayload.builder()
                .logId(UUID.randomUUID())
                .recipient("not-an-email")
                .subject("Welcome")
                .templateName("welcome-template")
                .build();

        // Validate
        Set<ConstraintViolation<EmailPayload>> violations = validator.validate(payload);
        assertFalse(violations.isEmpty(), "Invalid email should cause violation");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("recipient")));
    }

    @Test
    void testBlankRecipient() {
        // Create payload with blank recipient
        EmailPayload payload = EmailPayload.builder()
                .logId(UUID.randomUUID())
                .recipient("")
                .subject("Welcome")
                .templateName("welcome-template")
                .build();

        // Validate
        Set<ConstraintViolation<EmailPayload>> violations = validator.validate(payload);
        assertFalse(violations.isEmpty(), "Blank recipient should cause violation");
    }

    @Test
    void testBlankSubject() {
        // Create payload with blank subject
        EmailPayload payload = EmailPayload.builder()
                .logId(UUID.randomUUID())
                .recipient("user@example.com")
                .subject("")
                .templateName("welcome-template")
                .build();

        // Validate
        Set<ConstraintViolation<EmailPayload>> violations = validator.validate(payload);
        assertFalse(violations.isEmpty(), "Blank subject should cause violation");
    }

    @Test
    void testBlankTemplateName() {
        // Create payload with blank templateName
        EmailPayload payload = EmailPayload.builder()
                .logId(UUID.randomUUID())
                .recipient("user@example.com")
                .subject("Welcome")
                .templateName("")
                .build();

        // Validate
        Set<ConstraintViolation<EmailPayload>> violations = validator.validate(payload);
        assertFalse(violations.isEmpty(), "Blank template name should cause violation");
    }

    @Test
    void testToStringMasksEmail() {
        // Create payload
        EmailPayload payload = EmailPayload.builder()
                .logId(UUID.randomUUID())
                .recipient("john.doe@example.com")
                .subject("Welcome")
                .templateName("welcome-template")
                .build();

        // Get string representation
        String toStringResult = payload.toString();

        // Verify email is masked
        assertFalse(toStringResult.contains("john.doe"), "Email should be masked in toString()");
        assertTrue(toStringResult.contains("recipient_masked="), "Should use masked recipient field");
    }

    @Test
    void testToStringWithNullEmail() {
        // Create payload with null email
        EmailPayload payload = EmailPayload.builder()
                .logId(UUID.randomUUID())
                .recipient(null)
                .subject("Test")
                .templateName("template")
                .build();

        // Should not throw exception
        String toStringResult = payload.toString();
        assertTrue(toStringResult.contains("***"), "Should handle null email gracefully");
    }

    @Test
    void testOptionalVariables() {
        // Create payload without variables
        EmailPayload payload = EmailPayload.builder()
                .logId(UUID.randomUUID())
                .recipient("user@example.com")
                .subject("Welcome")
                .templateName("welcome-template")
                .build();

        // Validate - variables are optional
        Set<ConstraintViolation<EmailPayload>> violations = validator.validate(payload);
        assertTrue(violations.isEmpty(), "Payload without variables should be valid");
    }
}
