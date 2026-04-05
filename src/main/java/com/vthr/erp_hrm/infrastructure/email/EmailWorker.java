package com.vthr.erp_hrm.infrastructure.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vthr.erp_hrm.core.model.EmailStatus;
import com.vthr.erp_hrm.infrastructure.persistence.entity.EmailLogEntity;
import com.vthr.erp_hrm.infrastructure.persistence.repository.EmailLogRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailWorker {

    private final StringRedisTemplate redisTemplate;
    private final EmailLogRepository emailLogRepository;
    private final MailRenderingService mailRenderingService;
    private final JavaMailSender javaMailSender;
    private final ObjectMapper objectMapper;

    private static final String EMAIL_QUEUE = "email_notifications";
    private static final String EMAIL_DLQ = "email_dlq";

    @Value("${app.mail.from:noreply@vthr.com}")
    private String fromEmail;

    @Value("${app.mail.from-name:VTHR Solutions}")
    private String fromName;

    @Scheduled(fixedDelay = 5000)
    public void processEmailQueue() {
        try {
            String jsonPayload = redisTemplate.opsForList().rightPop(EMAIL_QUEUE, 2, TimeUnit.SECONDS);

            if (jsonPayload != null) {
                EmailPayload payload = objectMapper.readValue(jsonPayload, EmailPayload.class);
                EmailLogEntity logEntity = emailLogRepository.findById(payload.getLogId()).orElse(null);

                try {
                    sendEmail(payload);

                    if (logEntity != null) {
                        logEntity.setStatus(EmailStatus.SENT);
                        logEntity.setSentAt(ZonedDateTime.now());
                        emailLogRepository.save(logEntity);
                    }
                    log.info("Email sent: to='{}' subject='{}'", payload.getRecipient(), payload.getSubject());

                } catch (Exception e) {
                    handleSendError(payload, logEntity, e);
                }
            }
        } catch (Exception e) {
            log.error("Error in email queue worker: {}", e.getMessage());
        }
    }

    private void sendEmail(EmailPayload payload) throws Exception {
        String htmlBody = mailRenderingService.renderEmail(payload.getTemplateName(), payload.getVariables());

        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail, fromName);
        helper.setTo(payload.getRecipient());
        helper.setSubject(payload.getSubject());
        helper.setText(htmlBody, true);

        javaMailSender.send(message);
    }

    private void handleSendError(EmailPayload payload, EmailLogEntity logEntity, Exception e) {
        String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        log.warn("Failed to send email: to='{}' subject='{}' error='{}'", 
                payload.getRecipient(), payload.getSubject(), errorMsg);

        if (logEntity != null) {
            logEntity.setStatus(EmailStatus.FAILED);
            logEntity.setErrorMessage(errorMsg);
            emailLogRepository.save(logEntity);
        }

        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForList().leftPush(EMAIL_DLQ, jsonPayload);
            log.debug("Pushed failed email to dead-letter queue: to='{}'", payload.getRecipient());
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize payload for DLQ: {}", ex.getMessage());
        }
    }
}
