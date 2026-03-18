package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.core.model.Interview;
import com.vthr.erp_hrm.core.repository.InterviewRepository;
import com.vthr.erp_hrm.infrastructure.persistence.entity.InterviewEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InterviewRepositoryImpl implements InterviewRepository {

    private final InterviewJpaRepository jpaRepository;

    @Override
    public Optional<Interview> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Interview> findByApplicationId(UUID applicationId) {
        return jpaRepository.findByApplicationId(applicationId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Interview save(Interview interview) {
        InterviewEntity entity = toEntity(interview);
        return toDomain(jpaRepository.save(entity));
    }

    private Interview toDomain(InterviewEntity entity) {
        if (entity == null) return null;
        return Interview.builder()
                .id(entity.getId())
                .applicationId(entity.getApplicationId())
                .interviewTime(entity.getInterviewTime())
                .locationOrLink(entity.getLocationOrLink())
                .interviewerId(entity.getInterviewerId())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private InterviewEntity toEntity(Interview domain) {
        if (domain == null) return null;
        InterviewEntity entity = new InterviewEntity();
        entity.setId(domain.getId());
        entity.setApplicationId(domain.getApplicationId());
        entity.setInterviewTime(domain.getInterviewTime());
        entity.setLocationOrLink(domain.getLocationOrLink());
        entity.setInterviewerId(domain.getInterviewerId());
        if (domain.getStatus() != null) {
            entity.setStatus(domain.getStatus());
        } else {
            entity.setStatus("SCHEDULED");
        }
        return entity;
    }
}
