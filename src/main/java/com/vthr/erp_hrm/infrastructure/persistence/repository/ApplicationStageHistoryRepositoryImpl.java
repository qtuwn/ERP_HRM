package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.core.model.ApplicationStageHistory;
import com.vthr.erp_hrm.core.repository.ApplicationStageHistoryRepository;
import com.vthr.erp_hrm.infrastructure.persistence.entity.ApplicationStageHistoryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ApplicationStageHistoryRepositoryImpl implements ApplicationStageHistoryRepository {

    private final ApplicationStageHistoryJpaRepository jpaRepository;

    @Override
    public List<ApplicationStageHistory> findByApplicationIdOrderByCreatedAtDesc(UUID applicationId) {
        return jpaRepository.findByApplicationIdOrderByCreatedAtDesc(applicationId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public ApplicationStageHistory save(ApplicationStageHistory history) {
        ApplicationStageHistoryEntity entity = toEntity(history);
        return toDomain(jpaRepository.save(entity));
    }

    private ApplicationStageHistory toDomain(ApplicationStageHistoryEntity entity) {
        if (entity == null) return null;
        return ApplicationStageHistory.builder()
                .id(entity.getId())
                .applicationId(entity.getApplicationId())
                .fromStage(entity.getFromStage())
                .toStage(entity.getToStage())
                .changedBy(entity.getChangedBy())
                .note(entity.getNote())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private ApplicationStageHistoryEntity toEntity(ApplicationStageHistory domain) {
        if (domain == null) return null;
        ApplicationStageHistoryEntity entity = new ApplicationStageHistoryEntity();
        entity.setId(domain.getId());
        entity.setApplicationId(domain.getApplicationId());
        entity.setFromStage(domain.getFromStage());
        entity.setToStage(domain.getToStage());
        entity.setChangedBy(domain.getChangedBy());
        entity.setNote(domain.getNote());
        return entity;
    }
}
