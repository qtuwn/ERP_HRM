package com.vthr.erp_hrm.infrastructure.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vthr.erp_hrm.core.model.EmailStatus;
import com.vthr.erp_hrm.infrastructure.persistence.entity.EmailLogEntity;
import com.vthr.erp_hrm.infrastructure.persistence.repository.EmailLogRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Scheduled(fixedDelay = 5000)
    public void processEmailQueue() {
        try {
            // Block temporarily for 2 seconds looking for items
            String jsonPayload = redisTemplate.opsForList().rightPop(EMAIL_QUEUE, 2, TimeUnit.SECONDS);

            if (jsonPayload != null) {
                EmailPayload payload = objectMapper.readValue(jsonPayload, EmailPayload.class);
                log.info("Processing email queue payload for {}", payload.getRecipient());

                // Lookup DB
                EmailLogEntity logEntity = emailLogRepository.findById(payload.getLogId()).orElse(null);

                try {
                    String htmlBody = mailRenderingService.renderEmail(payload.getTemplateName(), payload.getVariables());

                    MimeMessage message = javaMailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                    helper.setTo(payload.getRecipient());
                    helper.setSubject(payload.getSubject());
                    helper.setText(htmlBody, true); // true = isHtml

                    javaMailSender.send(message);

                    if (logEntity != null) {
                        logEntity.setStatus(EmailStatus.SENT);
                        logEntity.setSentAt(ZonedDateTime.now());
                        emailLogRepository.save(logEntity);
                    }
                    log.info("Successfully dispatched email: {}", payload.getSubject());

                } catch (Exception e) {
                    log.error("Failed to transmit email template natively: {}", e.getMessage(), e);
                    if (logEntity != null) {
                        logEntity.setStatus(EmailStatus.FAILED);
                        logEntity.setErrorMessage(e.getMessage());
                        emailLogRepository.save(logEntity);
                    }
                    // Optionally push back to queue or a Dead Letter Queue... keeping it simple for now.
                }
            }
        } catch (Exception e) {
            log.error("Error interacting with Redis Email Queue worker: {}", e.getMessage(), e);
        }
    }
}
