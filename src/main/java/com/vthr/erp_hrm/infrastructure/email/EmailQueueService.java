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

import java.util.ArrayList;
import java.util.List;
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
            EmailLogEntity logEntity = createAndSaveLogEntry(recipient, subject, templateName);
            pushToQueue(logEntity.getId(), recipient, subject, templateName, variables);
            log.debug("Enqueued email: to='{}' subject='{}'", recipient, subject);
        } catch (Exception e) {
            handleEnqueueError(recipient, subject, e);
        }
    }

    public void enqueueBatchEmails(List<EmailRequest> requests) {
        List<EmailLogEntity> logEntities = new ArrayList<>();
        
        // Batch save log entities first
        for (EmailRequest request : requests) {
            EmailLogEntity logEntity = new EmailLogEntity();
            logEntity.setRecipient(request.recipient());
            logEntity.setSubject(request.subject());
            logEntity.setTemplateName(request.templateName());
            logEntity.setStatus(EmailStatus.PENDING);
            logEntities.add(logEntity);
        }
        logEntities = emailLogRepository.saveAll(logEntities);

        // Then batch push to queue
        int successful = 0;
        for (int i = 0; i < logEntities.size(); i++) {
            try {
                EmailLogEntity logEntity = logEntities.get(i);
                EmailRequest request = requests.get(i);
                pushToQueue(logEntity.getId(), request.recipient(), request.subject(), request.templateName(), request.variables());
                successful++;
            } catch (Exception e) {
                log.warn("Failed to queue batch email index {}: {}", i, e.getMessage());
            }
        }
        log.info("Batch email enqueued: {}/{} successful", successful, requests.size());
    }

    private EmailLogEntity createAndSaveLogEntry(String recipient, String subject, String templateName) {
        EmailLogEntity logEntity = new EmailLogEntity();
        logEntity.setRecipient(recipient);
        logEntity.setSubject(subject);
        logEntity.setTemplateName(templateName);
        logEntity.setStatus(EmailStatus.PENDING);
        return emailLogRepository.save(logEntity);
    }

    private void pushToQueue(Object logId, String recipient, String subject, String templateName, Map<String, Object> variables) throws JsonProcessingException {
        EmailPayload payload = EmailPayload.builder()
                .logId((java.util.UUID) logId)
                .recipient(recipient)
                .subject(subject)
                .templateName(templateName)
                .variables(variables)
                .build();

        String json = objectMapper.writeValueAsString(payload);
        redisTemplate.opsForList().leftPush(EMAIL_QUEUE, json);
    }

    private void handleEnqueueError(String recipient, String subject, Exception e) {
        String errorMsg = e instanceof JsonProcessingException ? "JSON serialization failed" : "Queue operation failed";
        log.error("Failed to enqueue email: to='{}' subject='{}' error='{}'", recipient, subject, errorMsg);
    }

    public record EmailRequest(
            String recipient,
            String subject,
            String templateName,
            Map<String, Object> variables
    ) {}
}
