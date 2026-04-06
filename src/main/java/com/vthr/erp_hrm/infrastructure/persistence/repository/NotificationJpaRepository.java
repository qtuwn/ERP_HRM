package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.infrastructure.persistence.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.UUID;

@Repository
public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, UUID> {

    @Query("""
            SELECT n FROM NotificationEntity n
            WHERE n.userId = :userId
            ORDER BY (CASE WHEN n.readAt IS NULL THEN 0 ELSE 1 END), n.createdAt DESC
            """)
    Page<NotificationEntity> findForUserOrderUnreadFirst(@Param("userId") UUID userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(UUID userId);

    boolean existsByIdAndUserId(UUID id, UUID userId);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.readAt = :readAt WHERE n.id = :id AND n.userId = :userId AND n.readAt IS NULL")
    int markRead(@Param("userId") UUID userId, @Param("id") UUID id, @Param("readAt") ZonedDateTime readAt);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.readAt = :readAt WHERE n.userId = :userId AND n.readAt IS NULL")
    int markAllRead(@Param("userId") UUID userId, @Param("readAt") ZonedDateTime readAt);
}

