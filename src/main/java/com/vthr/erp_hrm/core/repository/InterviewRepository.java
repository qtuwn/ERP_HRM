package com.vthr.erp_hrm.core.repository;

import com.vthr.erp_hrm.core.model.Interview;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterviewRepository {
    Optional<Interview> findById(UUID id);
    List<Interview> findByApplicationId(UUID applicationId);
    Interview save(Interview interview);
}
