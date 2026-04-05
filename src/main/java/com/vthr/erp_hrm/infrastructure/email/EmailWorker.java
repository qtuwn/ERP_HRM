package com.vthr.erp_hrm.infrastructure.email;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Value("${app.mail.from:noreply@vthr.com}")
    private String fromEmail;

    @Value("${app.mail.from-name:VTHR Solutions}")
    private String fromName;

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
                    helper.setFrom(fromEmail, fromName);
                    helper.setTo(payload.getRecipient());
                    helper.setSubject(payload.getSubject());
                    helper.setText(htmlBody, true); // true = isHtml

                    log.info("About to send email via SMTP: to='{}' subject='{}' template='{}'",
                            payload.getRecipient(), payload.getSubject(), payload.getTemplateName());
                    javaMailSender.send(message);
                    log.info("MailSender.send completed: to='{}' subject='{}'", payload.getRecipient(), payload.getSubject());

                    if (logEntity != null) {
                        logEntity.setStatus(EmailStatus.SENT);
                        logEntity.setSentAt(ZonedDateTime.now());
                        emailLogRepository.save(logEntity);
                    }
                    log.info("Successfully dispatched email: {}", payload.getSubject());

                } catch (MessagingException e) {
                    log.error("MessagingException while sending email (full stacktrace). to='{}' subject='{}'",
                            payload.getRecipient(), payload.getSubject(), e);
                    if (logEntity != null) {
                        logEntity.setStatus(EmailStatus.FAILED);
                        logEntity.setErrorMessage(e.getMessage());
                        emailLogRepository.save(logEntity);
                    }
                    // Optionally push back to queue or a Dead Letter Queue... keeping it simple for now.
                } catch (Exception e) {
                    log.error("Unexpected exception while sending email (full stacktrace). to='{}' subject='{}'",
                            payload.getRecipient(), payload.getSubject(), e);
                    if (logEntity != null) {
                        logEntity.setStatus(EmailStatus.FAILED);
                        logEntity.setErrorMessage(e.getMessage());
                        emailLogRepository.save(logEntity);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error interacting with Redis Email Queue worker: {}", e.getMessage(), e);
        }
    }
}
