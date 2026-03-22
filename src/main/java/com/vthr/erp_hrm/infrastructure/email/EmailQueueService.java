package com.vthr.erp_hrm.infrastructure.email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vthr.erp_hrm.core.model.EmailStatus;
import com.vthr.erp_hrm.infrastructure.persistence.entity.EmailLogEntity;
import com.vthr.erp_hrm.infrastructure.persistence.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailQueueService {

    private final StringRedisTemplate redisTemplate;
    private final EmailLogRepository emailLogRepository;
    private final ObjectMapper objectMapper;

    private static final String EMAIL_QUEUE = "email_notifications";

    public void enqueueEmail(String recipient, String subject, String templateName, Map<String, Object> variables) {
        try {
            // First, create the PENDING log entry
            EmailLogEntity logEntity = new EmailLogEntity();
            logEntity.setRecipient(recipient);
            logEntity.setSubject(subject);
            logEntity.setTemplateName(templateName);
            logEntity.setStatus(EmailStatus.PENDING);
            logEntity = emailLogRepository.save(logEntity);

            // Construct payload mapping to the Log ID
            EmailPayload payload = EmailPayload.builder()
                    .logId(logEntity.getId())
                    .recipient(recipient)
                    .subject(subject)
                    .templateName(templateName)
                    .variables(variables)
                    .build();

            String json = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForList().leftPush(EMAIL_QUEUE, json);
            log.info("Enqueued email message for {} with template {} (Log ID Context: {})", recipient, templateName, logEntity.getId());
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize EmailPayload: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to push to Redis queue: {}", e.getMessage(), e);
        }
    }
}
