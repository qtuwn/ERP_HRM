package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.infrastructure.persistence.entity.MessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MessageJpaRepository extends JpaRepository<MessageEntity, UUID> {
    Page<MessageEntity> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId, Pageable pageable);
}
