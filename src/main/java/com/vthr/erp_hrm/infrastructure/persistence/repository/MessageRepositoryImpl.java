package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.core.model.Message;
import com.vthr.erp_hrm.core.repository.MessageRepository;
import com.vthr.erp_hrm.infrastructure.persistence.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MessageRepositoryImpl implements MessageRepository {

    private final MessageJpaRepository jpaRepository;
    private final MessageMapper mapper;

    @Override
    public Message save(Message message) {
        if (message.getId() == null) {
            message.setId(UUID.randomUUID());
        }
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(message)));
    }

    @Override
    public Page<Message> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId, Pageable pageable) {
        return jpaRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId, pageable)
                .map(mapper::toDomain);
    }
}
