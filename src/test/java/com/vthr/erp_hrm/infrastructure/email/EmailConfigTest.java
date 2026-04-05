package com.vthr.erp_hrm.infrastructure.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailConfigTest {

    private EmailConfig config;

    @BeforeEach
    void setUp() {
        config = new EmailConfig();
        setDefaultFields();
    }

    @Test
    void javaMailSender_shouldThrowWhenPortInvalid() {
        ReflectionTestUtils.setField(config, "mailPort", 0);

        assertThrows(IllegalStateException.class, () -> config.javaMailSender());
    }

    @Test
    void javaMailSender_shouldThrowWhenHostMissing() {
        ReflectionTestUtils.setField(config, "mailHost", " ");

        assertThrows(IllegalStateException.class, () -> config.javaMailSender());
    }

    @Test
    void javaMailSender_shouldThrowWhenAuthEnabledAndUsernameMissing() {
        ReflectionTestUtils.setField(config, "mailUsername", "");
        ReflectionTestUtils.setField(config, "smtpAuth", true);

        assertThrows(IllegalStateException.class, () -> config.javaMailSender());
    }

    @Test
    void javaMailSender_shouldCreateSenderWhenAuthDisabledWithoutCredentials() {
        ReflectionTestUtils.setField(config, "smtpAuth", false);
        ReflectionTestUtils.setField(config, "mailUsername", "");
        ReflectionTestUtils.setField(config, "mailPassword", "");

        JavaMailSender sender = config.javaMailSender();

        assertInstanceOf(JavaMailSenderImpl.class, sender);
        JavaMailSenderImpl impl = (JavaMailSenderImpl) sender;
        assertEquals("smtp.example.com", impl.getHost());
        assertEquals(587, impl.getPort());
        assertEquals("false", impl.getJavaMailProperties().get("mail.smtp.auth"));
    }

    private void setDefaultFields() {
        ReflectionTestUtils.setField(config, "mailHost", "smtp.example.com");
        ReflectionTestUtils.setField(config, "mailPort", 587);
        ReflectionTestUtils.setField(config, "mailUsername", "user@example.com");
        ReflectionTestUtils.setField(config, "mailPassword", "app-password");
        ReflectionTestUtils.setField(config, "smtpAuth", true);
        ReflectionTestUtils.setField(config, "smtpStartTlsEnable", true);
        ReflectionTestUtils.setField(config, "smtpStartTlsRequired", true);
        ReflectionTestUtils.setField(config, "smtpSslTrust", "smtp.example.com");
        ReflectionTestUtils.setField(config, "smtpConnectionTimeout", "5000");
        ReflectionTestUtils.setField(config, "smtpTimeout", "5000");
        ReflectionTestUtils.setField(config, "smtpWriteTimeout", "5000");
    }
}
