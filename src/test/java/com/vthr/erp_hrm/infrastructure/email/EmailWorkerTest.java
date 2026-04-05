package com.vthr.erp_hrm.infrastructure.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vthr.erp_hrm.core.model.EmailStatus;
import com.vthr.erp_hrm.infrastructure.persistence.entity.EmailLogEntity;
import com.vthr.erp_hrm.infrastructure.persistence.repository.EmailLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailWorkerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    @Mock
    private EmailLogRepository emailLogRepository;

    @Mock
    private MailRenderingService mailRenderingService;

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EmailWorker emailWorker;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailWorker, "fromEmail", "test@example.com");
        ReflectionTestUtils.setField(emailWorker, "fromName", "Test App");
    }

    @Test
    void testProcessEmailQueue_NoQueueItems() {
        // Setup
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.rightPop(eq("email_notifications"), eq(2), any())).thenReturn(null);

        // Execute
        emailWorker.processEmailQueue();

        // Verify no processing occurs
        verify(emailLogRepository, never()).findById(any());
        verify(javaMailSender, never()).createMimeMessage();
    }

    @Test
    void testProcessEmailQueue_HandlesJsonDeserializationError() throws Exception {
        // Setup
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.rightPop(eq("email_notifications"), eq(2), any())).thenReturn("invalid json");
        when(objectMapper.readValue("invalid json", EmailPayload.class)).thenThrow(new com.fasterxml.jackson.core.JsonParseException(null, "Invalid JSON"));

        // Execute
        emailWorker.processEmailQueue();

        // Verify error was logged
        verify(javaMailSender, never()).createMimeMessage();
    }
}
