package com.vthr.erp_hrm.infrastructure.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vthr.erp_hrm.infrastructure.persistence.entity.WebhookOutboxEntity;
import com.vthr.erp_hrm.infrastructure.persistence.repository.WebhookOutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookDispatcherWorker {
    private final WebhookOutboxJpaRepository repo;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Value("${app.webhook.enabled:false}")
    private boolean enabled;

    @Value("${app.webhook.url:}")
    private String webhookUrl;

    @Value("${app.webhook.secret:}")
    private String webhookSecret;

    @Value("${app.webhook.batch-size:50}")
    private int batchSize;

    @Value("${app.webhook.max-attempts:10}")
    private int maxAttempts;

    @Value("${app.webhook.timeout-ms:3000}")
    private int timeoutMs;

    @Scheduled(fixedDelayString = "${app.webhook.worker.delay-ms:2000}")
    @Transactional
    public void dispatchDue() {
        if (!enabled) return;
        if (webhookUrl == null || webhookUrl.isBlank()) return;

        ZonedDateTime now = ZonedDateTime.now(clock);
        List<WebhookOutboxEntity> due = repo.findDueForSending(
                List.of(WebhookOutboxStatus.PENDING, WebhookOutboxStatus.FAILED),
                now,
                PageRequest.of(0, Math.max(1, batchSize))
        );

        if (due.isEmpty()) return;

        RestClient client = buildClient();

        for (WebhookOutboxEntity item : due) {
            if (item.getAttempts() >= maxAttempts) {
                continue;
            }

            item.setStatus(WebhookOutboxStatus.SENDING);
            item.setAttempts(item.getAttempts() + 1);
            item.setLastError(null);
            repo.save(item);

            try {
                String body = objectMapper.writeValueAsString(new WebhookEnvelope(item.getEventType(), item.getPayloadJson()));

                client.post()
                        .uri(webhookUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Event-Type", item.getEventType())
                        .header("X-Webhook-Secret", webhookSecret == null ? "" : webhookSecret)
                        .body(body)
                        .retrieve()
                        .toBodilessEntity();

                item.setStatus(WebhookOutboxStatus.SENT);
                item.setNextAttemptAt(now);
                repo.save(item);
            } catch (Exception ex) {
                String msg = ex.getMessage() != null ? ex.getMessage() : "Failed";
                item.setStatus(WebhookOutboxStatus.FAILED);
                item.setLastError(msg);
                item.setNextAttemptAt(now.plusSeconds(backoffSeconds(item.getAttempts())));
                repo.save(item);
                log.warn("Webhook dispatch failed id={} attempts={} err={}", item.getId(), item.getAttempts(), msg);
            }
        }
    }

    private RestClient buildClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private static long backoffSeconds(int attempts) {
        long base = 2L;
        long max = 60L;
        long delay = (long) Math.min(max, base * Math.pow(2, Math.max(0, attempts - 1)));
        return Math.max(2L, delay);
    }

    record WebhookEnvelope(String eventType, Object payload) {
    }
}

