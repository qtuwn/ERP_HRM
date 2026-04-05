package com.vthr.erp_hrm.infrastructure.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vthr.erp_hrm.infrastructure.persistence.entity.WebhookOutboxEntity;
import com.vthr.erp_hrm.infrastructure.persistence.repository.WebhookOutboxJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class WebhookDispatcherWorkerTest {

    @Mock
    private WebhookOutboxJpaRepository repo;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final PlatformTransactionManager transactionManager = new NoOpTransactionManager();

    private RestClient restClient;
    private MockRestServiceServer server;
    private WebhookDispatcherWorker worker;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
        worker = new WebhookDispatcherWorker(repo, objectMapper, clock, transactionManager, restClient);
        ReflectionTestUtils.setField(worker, "enabled", true);
        ReflectionTestUtils.setField(worker, "webhookUrl", "http://localhost/webhook");
        ReflectionTestUtils.setField(worker, "webhookSecret", "secret-123");
        ReflectionTestUtils.setField(worker, "batchSize", 50);
        ReflectionTestUtils.setField(worker, "maxAttempts", 10);
    }

    @Test
    void dispatchDue_shouldNoopWhenDisabled() {
        ReflectionTestUtils.setField(worker, "enabled", false);

        worker.dispatchDue();

        verify(repo, never()).findDueForSending(anyList(), any(), anyInt(), any());
    }

    @Test
    void dispatchDue_shouldSendWebhookAndUpdateStatus() {
        ZonedDateTime now = ZonedDateTime.now(clock);
        WebhookOutboxEntity item = WebhookOutboxEntity.builder()
                .id(UUID.randomUUID())
                .eventType("application:applied")
                .payloadJson("{\"applicationId\":\"app-1\"}")
                .status(WebhookOutboxStatus.PENDING)
                .attempts(0)
                .nextAttemptAt(now)
                .build();

        when(repo.findDueForSending(eq(List.of(WebhookOutboxStatus.PENDING, WebhookOutboxStatus.FAILED)), eq(now), eq(10), any()))
                .thenReturn(List.of(item));

        server.expect(requestTo("http://localhost/webhook"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Event-Type", "application:applied"))
                .andExpect(header("X-Webhook-Secret", "secret-123"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "eventType": "application:applied",
                          "payload": {
                            "applicationId": "app-1"
                          }
                        }
                        """))
                .andRespond(withSuccess());

        worker.dispatchDue();

        server.verify();
        verify(repo).saveAll(anyList());
        verify(repo).save(eq(item));
    }

    private static final class NoOpTransactionManager implements PlatformTransactionManager {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}
