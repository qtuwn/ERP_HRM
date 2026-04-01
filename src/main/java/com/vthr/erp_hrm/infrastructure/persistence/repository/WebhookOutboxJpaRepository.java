package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.infrastructure.persistence.entity.WebhookOutboxEntity;
import com.vthr.erp_hrm.infrastructure.webhook.WebhookOutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookOutboxJpaRepository extends JpaRepository<WebhookOutboxEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o FROM WebhookOutboxEntity o
            WHERE o.status IN :statuses
              AND o.nextAttemptAt <= :now
            ORDER BY o.createdAt ASC
            """)
    List<WebhookOutboxEntity> findDueForSending(
            @Param("statuses") List<WebhookOutboxStatus> statuses,
            @Param("now") ZonedDateTime now,
            Pageable pageable
    );
}

