package com.vthr.erp_hrm.core.repository;

import com.vthr.erp_hrm.core.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MessageRepository {
    Message save(Message message);
    Page<Message> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId, Pageable pageable);
}
