package com.vthr.erp_hrm.infrastructure.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vthr.erp_hrm.core.model.EmailStatus;
import com.vthr.erp_hrm.infrastructure.persistence.entity.EmailLogEntity;
import com.vthr.erp_hrm.infrastructure.persistence.repository.EmailLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailQueueServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    @Mock
    private EmailLogRepository emailLogRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EmailQueueService emailQueueService;

    @Test
    void testEnqueueEmail_CreatesLogAndQueuesMessage() throws Exception {
        // Setup
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        
        UUID logId = UUID.randomUUID();
        EmailLogEntity savedEntity = new EmailLogEntity();
        savedEntity.setId(logId);
        when(emailLogRepository.save(any())).thenReturn(savedEntity);
        
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"logId\":\"" + logId + "\"}");

        // Execute
        emailQueueService.enqueueEmail("user@example.com", "Welcome", "welcome-template", new HashMap<>());

        // Verify log entry created with PENDING status
        ArgumentCaptor<EmailLogEntity> captor = ArgumentCaptor.forClass(EmailLogEntity.class);
        verify(emailLogRepository).save(captor.capture());
        EmailLogEntity saved = captor.getValue();
        assertEquals("user@example.com", saved.getRecipient());
        assertEquals(EmailStatus.PENDING, saved.getStatus());

        // Verify queued to Redis
        verify(listOperations).leftPush(eq("email_notifications"), anyString());
    }

    @Test
    void testEnqueueEmail_HandlesSerializationError() throws Exception {
        // Setup: email log is saved successfully, but JSON serialization fails
        EmailLogEntity savedEntity = new EmailLogEntity();
        savedEntity.setId(UUID.randomUUID());
        when(emailLogRepository.save(any())).thenReturn(savedEntity);
        when(objectMapper.writeValueAsString(any())).thenThrow(new com.fasterxml.jackson.core.JsonGenerationException("JSON error"));

        // Execute (should not throw, error is caught and logged)
        emailQueueService.enqueueEmail("user@example.com", "Welcome", "welcome-template", new HashMap<>());

        // Verify log entry was still created
        verify(emailLogRepository).save(any());
        // Verify no queue operation occurred
        verify(listOperations, never()).leftPush(anyString(), anyString());
    }

    @Test
    void testEnqueueBatchEmails_SavesAllAndQueuesAll() throws Exception {
        // Setup
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        
        List<EmailQueueService.EmailRequest> requests = new ArrayList<>();
        requests.add(new EmailQueueService.EmailRequest("user1@example.com", "Welcome", "template1", new HashMap<>()));
        requests.add(new EmailQueueService.EmailRequest("user2@example.com", "Welcome", "template2", new HashMap<>()));
        
        List<EmailLogEntity> savedEntities = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            EmailLogEntity entity = new EmailLogEntity();
            entity.setId(UUID.randomUUID());
            savedEntities.add(entity);
        }
        when(emailLogRepository.saveAll(any())).thenReturn(savedEntities);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Execute
        emailQueueService.enqueueBatchEmails(requests);

        // Verify batch save
        ArgumentCaptor<List<EmailLogEntity>> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(emailLogRepository).saveAll(saveCaptor.capture());
        assertEquals(2, saveCaptor.getValue().size());

        // Verify all queued
        verify(listOperations, times(2)).leftPush(eq("email_notifications"), anyString());
    }

    @Test
    void testEnqueueBatchEmails_PartialFailureLogsWarning() throws Exception {
        // Setup
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        
        List<EmailQueueService.EmailRequest> requests = new ArrayList<>();
        requests.add(new EmailQueueService.EmailRequest("user1@example.com", "Welcome", "template1", new HashMap<>()));
        requests.add(new EmailQueueService.EmailRequest("user2@example.com", "Welcome", "template2", new HashMap<>()));
        
        List<EmailLogEntity> savedEntities = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            EmailLogEntity entity = new EmailLogEntity();
            entity.setId(UUID.randomUUID());
            savedEntities.add(entity);
        }
        when(emailLogRepository.saveAll(any())).thenReturn(savedEntities);
        
        // First call succeeds, second fails
        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{}")
                .thenThrow(new com.fasterxml.jackson.core.JsonGenerationException("JSON error"));

        // Execute
        emailQueueService.enqueueBatchEmails(requests);

        // Verify only one queued (first succeeded, second failed)
        verify(listOperations, times(1)).leftPush(eq("email_notifications"), anyString());
    }
}
